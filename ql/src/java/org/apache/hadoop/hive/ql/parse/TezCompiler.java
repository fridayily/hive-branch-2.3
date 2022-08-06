/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.hadoop.hive.ql.parse;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.hive.ql.exec.*;
import org.apache.hadoop.hive.ql.lib.*;
import org.apache.hadoop.hive.ql.plan.*;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFBloomFilter.GenericUDAFBloomFilterEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.Context;
import org.apache.hadoop.hive.ql.QueryState;
import org.apache.hadoop.hive.ql.exec.tez.TezTask;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.log.PerfLogger;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.optimizer.ConstantPropagate;
import org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcCtx.ConstantPropagateOption;
import org.apache.hadoop.hive.ql.optimizer.ConvertJoinMapJoin;
import org.apache.hadoop.hive.ql.optimizer.DynamicPartitionPruningOptimization;
import org.apache.hadoop.hive.ql.optimizer.MergeJoinProc;
import org.apache.hadoop.hive.ql.optimizer.ReduceSinkMapJoinProc;
import org.apache.hadoop.hive.ql.optimizer.RemoveDynamicPruningBySize;
import org.apache.hadoop.hive.ql.optimizer.SetReducerParallelism;
import org.apache.hadoop.hive.ql.optimizer.metainfo.annotation.AnnotateWithOpTraits;
import org.apache.hadoop.hive.ql.optimizer.physical.AnnotateRunTimeStatsOptimizer;
import org.apache.hadoop.hive.ql.optimizer.physical.CrossProductCheck;
import org.apache.hadoop.hive.ql.optimizer.physical.LlapDecider;
import org.apache.hadoop.hive.ql.optimizer.physical.MemoryDecider;
import org.apache.hadoop.hive.ql.optimizer.physical.MetadataOnlyOptimizer;
import org.apache.hadoop.hive.ql.optimizer.physical.NullScanOptimizer;
import org.apache.hadoop.hive.ql.optimizer.physical.PhysicalContext;
import org.apache.hadoop.hive.ql.optimizer.physical.SerializeFilter;
import org.apache.hadoop.hive.ql.optimizer.physical.StageIDsRearranger;
import org.apache.hadoop.hive.ql.optimizer.physical.Vectorizer;
import org.apache.hadoop.hive.ql.optimizer.stats.annotation.AnnotateWithStatistics;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.hive.ql.session.SessionState.LogHelper;
import org.apache.hadoop.hive.ql.exec.SelectOperator;
import org.apache.hadoop.hive.ql.stats.StatsUtils;

/**
 * TezCompiler translates the operator plan into TezTasks.
 */
public class TezCompiler extends TaskCompiler {

  protected static final Logger LOG = LoggerFactory.getLogger(TezCompiler.class);

  public TezCompiler() {
  }

  @Override
  public void init(QueryState queryState, LogHelper console, Hive db) {
    super.init(queryState, console, db);

    // Tez requires us to use RPC for the query plan
    HiveConf.setBoolVar(conf, ConfVars.HIVE_RPC_QUERY_PLAN, true);

    // We require the use of recursive input dirs for union processing
    conf.setBoolean("mapred.input.dir.recursive", true);
  }

  @Override
  protected void optimizeOperatorPlan(ParseContext pCtx, Set<ReadEntity> inputs,
      Set<WriteEntity> outputs) throws SemanticException {
    PerfLogger perfLogger = SessionState.getPerfLogger();
    // Create the context for the walker
    OptimizeTezProcContext procCtx = new OptimizeTezProcContext(conf, pCtx, inputs, outputs);
    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // setup dynamic partition pruning where possible
    runDynamicPartitionPruning(procCtx, inputs, outputs);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Setup dynamic partition pruning");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // setup stats in the operator plan
    runStatsAnnotation(procCtx);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Setup stats in the operator plan");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // run the optimizations that use stats for optimization
    runStatsDependentOptimizations(procCtx, inputs, outputs);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Run the optimizations that use stats for optimization");

    // Removing semijoin optimization when it may not be beneficial
    removeSemijoinOptimizationByBenefit(procCtx);

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // Remove any parallel edge between semijoin and mapjoin.
    removeSemijoinsParallelToMapJoin(procCtx);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Run the optimizations that use stats for optimization");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // Remove semijoin optimization if it creates a cycle with mapside joins
    removeSemiJoinCyclesDueToMapsideJoins(procCtx);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Remove semijoin optimizations if it creates a cycle with mapside join");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // Remove semijoin optimization if SMB join is created.
    removeSemijoinOptimizationFromSMBJoins(procCtx);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Remove semijoin optimizations if needed");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // Remove bloomfilter if no stats generated
    removeSemiJoinIfNoStats(procCtx);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Remove bloom filter optimizations if needed");

    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    // after the stats phase we might have some cyclic dependencies that we need
    // to take care of.
    runCycleAnalysisForPartitionPruning(procCtx, inputs, outputs);
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "Run cycle analysis for partition pruning");

    // need a new run of the constant folding because we might have created lots
    // of "and true and true" conditions.
    // Rather than run the full constant folding just need to shortcut AND/OR expressions
    // involving constant true/false values.
    if(procCtx.conf.getBoolVar(ConfVars.HIVEOPTCONSTANTPROPAGATION)) {
      new ConstantPropagate(ConstantPropagateOption.SHORTCUT).transform(procCtx.parseContext);
    }

  }

  private void runCycleAnalysisForPartitionPruning(OptimizeTezProcContext procCtx,
      Set<ReadEntity> inputs, Set<WriteEntity> outputs) throws SemanticException {

    if (!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_PARTITION_PRUNING)) {
      return;
    }

    boolean cycleFree = false;
    while (!cycleFree) {
      cycleFree = true;
      Set<Set<Operator<?>>> components = getComponents(procCtx);
      for (Set<Operator<?>> component : components) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Component: ");
          for (Operator<?> co : component) {
            LOG.debug("Operator: " + co.getName() + ", " + co.getIdentifier());
          }
        }
        if (component.size() != 1) {
          LOG.info("Found cycle in operator plan...");
          cycleFree = false;
          removeCycleOperator(component, procCtx);
          break;
        }
      }
      LOG.info("Cycle free: " + cycleFree);
    }
  }

  private void removeCycleOperator(Set<Operator<?>> component, OptimizeTezProcContext context) throws SemanticException {
    AppMasterEventOperator victimAM = null;
    TableScanOperator victimTS = null;
    ReduceSinkOperator victimRS = null;

    for (Operator<?> o : component) {
      // Look for AppMasterEventOperator or ReduceSinkOperator
      if (o instanceof AppMasterEventOperator) {
        if (victimAM == null
                || o.getStatistics().getDataSize() < victimAM.getStatistics()
                .getDataSize()) {
          victimAM = (AppMasterEventOperator) o;
        }
      } else if (o instanceof ReduceSinkOperator) {
        TableScanOperator ts = context.parseContext.getRsOpToTsOpMap().get(o);
        if (ts == null) {
          continue;
        }
        // Sanity check
        assert component.contains(ts);

        if (victimRS == null ||
                ts.getStatistics().getDataSize() <
                victimTS.getStatistics().getDataSize()) {
            victimRS = (ReduceSinkOperator) o;
            victimTS = ts;
          }
        }
      }

    // Always set the min/max optimization as victim.
    Operator<?> victim = victimRS;

    if (victimRS == null && victimAM != null ) {
        victim = victimAM;
    } else if (victimAM == null) {
      // do nothing
    } else {
      // Cycle consists of atleast one dynamic partition pruning(DPP)
      // optimization and atleast one min/max optimization.
      // DPP is a better optimization unless it ends up scanning the
      // bigger table for keys instead of the smaller table.

      // Get the parent TS of victimRS.
      Operator<?> op = victimRS;
      while(!(op instanceof TableScanOperator)) {
        op = op.getParentOperators().get(0);
      }
      if ((2 * op.getStatistics().getDataSize()) <
              victimAM.getStatistics().getDataSize()) {
        victim = victimAM;
      }
    }

    if (victim == null ||
            (!context.pruningOpsRemovedByPriorOpt.isEmpty() &&
                    context.pruningOpsRemovedByPriorOpt.contains(victim))) {
      return;
    }

    GenTezUtils.removeBranch(victim);

    if (victim == victimRS) {
      GenTezUtils.removeSemiJoinOperator(context.parseContext, victimRS, victimTS);
    }
    return;
  }

  // Tarjan's algo
  private Set<Set<Operator<?>>> getComponents(OptimizeTezProcContext procCtx) {
    Deque<Operator<?>> deque = new LinkedList<Operator<?>>();
    deque.addAll(procCtx.parseContext.getTopOps().values());

    AtomicInteger index = new AtomicInteger();
    Map<Operator<?>, Integer> indexes = new HashMap<Operator<?>, Integer>();
    Map<Operator<?>, Integer> lowLinks = new HashMap<Operator<?>, Integer>();
    Stack<Operator<?>> nodes = new Stack<Operator<?>>();
    Set<Set<Operator<?>>> components = new LinkedHashSet<Set<Operator<?>>>();

    for (Operator<?> o : deque) {
      if (!indexes.containsKey(o)) {
        connect(o, index, nodes, indexes, lowLinks, components, procCtx.parseContext);
      }
    }

    return components;
  }

  private void connect(Operator<?> o, AtomicInteger index, Stack<Operator<?>> nodes,
      Map<Operator<?>, Integer> indexes, Map<Operator<?>, Integer> lowLinks,
      Set<Set<Operator<?>>> components, ParseContext parseContext) {

    indexes.put(o, index.get());
    lowLinks.put(o, index.get());
    index.incrementAndGet();
    nodes.push(o);

    List<Operator<?>> children;
    if (o instanceof AppMasterEventOperator) {
      children = new ArrayList<Operator<?>>();
      children.addAll(o.getChildOperators());
      TableScanOperator ts = ((DynamicPruningEventDesc) o.getConf()).getTableScan();
      LOG.debug("Adding special edge: " + o.getName() + " --> " + ts.toString());
      children.add(ts);
    } else if (o instanceof ReduceSinkOperator){
      // min/max case
      children = new ArrayList<Operator<?>>();
      children.addAll(o.getChildOperators());
      TableScanOperator ts = parseContext.getRsOpToTsOpMap().get(o);
      if (ts != null) {
        LOG.debug("Adding special edge: " + o.getName() + " --> " + ts.toString());
        children.add(ts);
      }
    } else {
      children = o.getChildOperators();
    }

    for (Operator<?> child : children) {
      if (!indexes.containsKey(child)) {
        connect(child, index, nodes, indexes, lowLinks, components, parseContext);
        lowLinks.put(o, Math.min(lowLinks.get(o), lowLinks.get(child)));
      } else if (nodes.contains(child)) {
        lowLinks.put(o, Math.min(lowLinks.get(o), indexes.get(child)));
      }
    }

    if (lowLinks.get(o).equals(indexes.get(o))) {
      Set<Operator<?>> component = new LinkedHashSet<Operator<?>>();
      components.add(component);
      Operator<?> current;
      do {
        current = nodes.pop();
        component.add(current);
      } while (current != o);
    }
  }

  private void runStatsAnnotation(OptimizeTezProcContext procCtx) throws SemanticException {
    new AnnotateWithStatistics().transform(procCtx.parseContext);
    new AnnotateWithOpTraits().transform(procCtx.parseContext);
  }

  private void runStatsDependentOptimizations(OptimizeTezProcContext procCtx,
      Set<ReadEntity> inputs, Set<WriteEntity> outputs) throws SemanticException {

    // Sequence of TableScan operators to be walked
    Deque<Operator<?>> deque = new LinkedList<Operator<?>>();
    deque.addAll(procCtx.parseContext.getTopOps().values());

    // create a walker which walks the tree in a DFS manner while maintaining
    // the operator stack.
    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(new RuleRegExp("Set parallelism - ReduceSink",
        ReduceSinkOperator.getOperatorName() + "%"),
        new SetReducerParallelism());

    opRules.put(new RuleRegExp("Convert Join to Map-join",
        JoinOperator.getOperatorName() + "%"), new ConvertJoinMapJoin());

    opRules.put(
        new RuleRegExp("Remove dynamic pruning by size",
        AppMasterEventOperator.getOperatorName() + "%"),
        new RemoveDynamicPruningBySize());

    // The dispatcher fires the processor corresponding to the closest matching
    // rule and passes the context along
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, procCtx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(procCtx.parseContext.getTopOps().values());
    GraphWalker ogw = new ForwardWalker(disp);
    ogw.startWalking(topNodes, null);
  }

  private void runDynamicPartitionPruning(OptimizeTezProcContext procCtx, Set<ReadEntity> inputs,
      Set<WriteEntity> outputs) throws SemanticException {

    if (!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_PARTITION_PRUNING)) {
      return;
    }

    // Sequence of TableScan operators to be walked
    Deque<Operator<?>> deque = new LinkedList<Operator<?>>();
    deque.addAll(procCtx.parseContext.getTopOps().values());

    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(
        new RuleRegExp(new String("Dynamic Partition Pruning"), FilterOperator.getOperatorName()
            + "%"), new DynamicPartitionPruningOptimization());

    // The dispatcher fires the processor corresponding to the closest matching
    // rule and passes the context along
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, procCtx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(procCtx.parseContext.getTopOps().values());
    GraphWalker ogw = new ForwardWalker(disp);
    ogw.startWalking(topNodes, null);
  }

  @Override
  protected void generateTaskTree(List<Task<? extends Serializable>> rootTasks, ParseContext pCtx,
      List<Task<MoveWork>> mvTask, Set<ReadEntity> inputs, Set<WriteEntity> outputs)
      throws SemanticException {

	PerfLogger perfLogger = SessionState.getPerfLogger();
    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    ParseContext tempParseContext = getParseContext(pCtx, rootTasks);
    GenTezUtils utils = new GenTezUtils();
    GenTezWork genTezWork = new GenTezWork(utils);

    GenTezProcContext procCtx = new GenTezProcContext(
        conf, tempParseContext, mvTask, rootTasks, inputs, outputs);

    // create a walker which walks the tree in a DFS manner while maintaining
    // the operator stack.
    // The dispatcher generates the plan from the operator tree
    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(new RuleRegExp("Split Work - ReduceSink",
        ReduceSinkOperator.getOperatorName() + "%"),
        genTezWork);

    opRules.put(new RuleRegExp("No more walking on ReduceSink-MapJoin",
        MapJoinOperator.getOperatorName() + "%"), new ReduceSinkMapJoinProc());

    opRules.put(new RuleRegExp("Recognize a Sorted Merge Join operator to setup the right edge and"
        + " stop traversing the DummyStore-MapJoin", CommonMergeJoinOperator.getOperatorName()
        + "%"), new MergeJoinProc());

    opRules.put(new RuleRegExp("Split Work + Move/Merge - FileSink",
        FileSinkOperator.getOperatorName() + "%"),
        new CompositeProcessor(new FileSinkProcessor(), genTezWork));

    opRules.put(new RuleRegExp("Split work - DummyStore", DummyStoreOperator.getOperatorName()
        + "%"), genTezWork);

    opRules.put(new RuleRegExp("Handle Potential Analyze Command",
        TableScanOperator.getOperatorName() + "%"),
        new ProcessAnalyzeTable(utils));

    opRules.put(new RuleRegExp("Remember union",
        UnionOperator.getOperatorName() + "%"),
        new UnionProcessor());

    opRules.put(new RuleRegExp("AppMasterEventOperator",
        AppMasterEventOperator.getOperatorName() + "%"),
        new AppMasterEventProcessor());

    // The dispatcher fires the processor corresponding to the closest matching
    // rule and passes the context along
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, procCtx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(pCtx.getTopOps().values());
    GraphWalker ogw = new GenTezWorkWalker(disp, procCtx);
    ogw.startWalking(topNodes, null);

    // we need to specify the reserved memory for each work that contains Map Join
    for (List<BaseWork> baseWorkList : procCtx.mapJoinWorkMap.values()) {
      for (BaseWork w : baseWorkList) {
        // work should be the smallest unit for memory allocation
        w.setReservedMemoryMB(
            (int)(conf.getLongVar(ConfVars.HIVECONVERTJOINNOCONDITIONALTASKTHRESHOLD) / (1024 * 1024)));
      }
    }

    // we need to clone some operator plans and remove union operators still
    int indexForTezUnion = 0;
    for (BaseWork w: procCtx.workWithUnionOperators) {
      GenTezUtils.removeUnionOperators(procCtx, w, indexForTezUnion++);
    }

    // then we make sure the file sink operators are set up right
    for (FileSinkOperator fileSink: procCtx.fileSinkSet) {
      GenTezUtils.processFileSink(procCtx, fileSink);
    }

    // Connect any edges required for min/max pushdown
    if (pCtx.getRsToRuntimeValuesInfoMap().size() > 0) {
      for (ReduceSinkOperator rs : pCtx.getRsToRuntimeValuesInfoMap().keySet()) {
        // Process min/max
        GenTezUtils.processDynamicMinMaxPushDownOperator(
                procCtx, pCtx.getRsToRuntimeValuesInfoMap().get(rs), rs);
      }
    }
    // and finally we hook up any events that need to be sent to the tez AM
    LOG.debug("There are " + procCtx.eventOperatorSet.size() + " app master events.");
    for (AppMasterEventOperator event : procCtx.eventOperatorSet) {
      LOG.debug("Handling AppMasterEventOperator: " + event);
      GenTezUtils.processAppMasterEvent(procCtx, event);
    }
    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "generateTaskTree");
  }

  @Override
  protected void setInputFormat(Task<? extends Serializable> task) {
    if (task instanceof TezTask) {
      TezWork work = ((TezTask)task).getWork();
      List<BaseWork> all = work.getAllWork();
      for (BaseWork w: all) {
        if (w instanceof MapWork) {
          MapWork mapWork = (MapWork) w;
          HashMap<String, Operator<? extends OperatorDesc>> opMap = mapWork.getAliasToWork();
          if (!opMap.isEmpty()) {
            for (Operator<? extends OperatorDesc> op : opMap.values()) {
              setInputFormat(mapWork, op);
            }
          }
        }
      }
    } else if (task instanceof ConditionalTask) {
      List<Task<? extends Serializable>> listTasks
        = ((ConditionalTask) task).getListTasks();
      for (Task<? extends Serializable> tsk : listTasks) {
        setInputFormat(tsk);
      }
    }

    if (task.getChildTasks() != null) {
      for (Task<? extends Serializable> childTask : task.getChildTasks()) {
        setInputFormat(childTask);
      }
    }
  }

  private void setInputFormat(MapWork work, Operator<? extends OperatorDesc> op) {
    if (op == null) {
      return;
    }
    if (op.isUseBucketizedHiveInputFormat()) {
      work.setUseBucketizedHiveInputFormat(true);
      return;
    }

    if (op.getChildOperators() != null) {
      for (Operator<? extends OperatorDesc> childOp : op.getChildOperators()) {
        setInputFormat(work, childOp);
      }
    }
  }

  @Override
  protected void decideExecMode(List<Task<? extends Serializable>> rootTasks, Context ctx,
      GlobalLimitCtx globalLimitCtx)
      throws SemanticException {
    // currently all Tez work is on the cluster
    return;
  }

  @Override
  protected void optimizeTaskPlan(List<Task<? extends Serializable>> rootTasks, ParseContext pCtx,
      Context ctx) throws SemanticException {
    PerfLogger perfLogger = SessionState.getPerfLogger();
    perfLogger.PerfLogBegin(this.getClass().getName(), PerfLogger.TEZ_COMPILER);
    PhysicalContext physicalCtx = new PhysicalContext(conf, pCtx, pCtx.getContext(), rootTasks,
       pCtx.getFetchTask());

    if (conf.getBoolVar(HiveConf.ConfVars.HIVENULLSCANOPTIMIZE)) {
      physicalCtx = new NullScanOptimizer().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping null scan query optimization");
    }

    if (conf.getBoolVar(HiveConf.ConfVars.HIVEMETADATAONLYQUERIES)) {
      physicalCtx = new MetadataOnlyOptimizer().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping metadata only query optimization");
    }

    if (conf.getBoolVar(HiveConf.ConfVars.HIVE_CHECK_CROSS_PRODUCT)) {
      physicalCtx = new CrossProductCheck().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping cross product analysis");
    }

    if (conf.getBoolVar(HiveConf.ConfVars.HIVE_VECTORIZATION_ENABLED)
        && ctx.getExplainAnalyze() == null) {
      physicalCtx = new Vectorizer().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping vectorization");
    }

    if (!"none".equalsIgnoreCase(conf.getVar(HiveConf.ConfVars.HIVESTAGEIDREARRANGE))) {
      physicalCtx = new StageIDsRearranger().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping stage id rearranger");
    }

    if ((conf.getBoolVar(HiveConf.ConfVars.HIVE_TEZ_ENABLE_MEMORY_MANAGER))
        && (conf.getBoolVar(HiveConf.ConfVars.HIVEUSEHYBRIDGRACEHASHJOIN))) {
      physicalCtx = new MemoryDecider().resolve(physicalCtx);
    }

    if ("llap".equalsIgnoreCase(conf.getVar(HiveConf.ConfVars.HIVE_EXECUTION_MODE))) {
      physicalCtx = new LlapDecider().resolve(physicalCtx);
    } else {
      LOG.debug("Skipping llap decider");
    }

    //  This optimizer will serialize all filters that made it to the
    //  table scan operator to avoid having to do it multiple times on
    //  the backend. If you have a physical optimization that changes
    //  table scans or filters, you have to invoke it before this one.
    physicalCtx = new SerializeFilter().resolve(physicalCtx);

    if (physicalCtx.getContext().getExplainAnalyze() != null) {
      new AnnotateRunTimeStatsOptimizer().resolve(physicalCtx);
    }

    perfLogger.PerfLogEnd(this.getClass().getName(), PerfLogger.TEZ_COMPILER, "optimizeTaskPlan");
    return;
  }

  private static class SMBJoinOpProcContext implements NodeProcessorCtx {
    HashMap<CommonMergeJoinOperator, TableScanOperator> JoinOpToTsOpMap = new HashMap<CommonMergeJoinOperator, TableScanOperator>();
  }

  private static class SMBJoinOpProc implements NodeProcessor {

    @Override
    public Object process(Node nd, Stack<Node> stack, NodeProcessorCtx procCtx,
                          Object... nodeOutputs) throws SemanticException {
      SMBJoinOpProcContext ctx = (SMBJoinOpProcContext) procCtx;
      ctx.JoinOpToTsOpMap.put((CommonMergeJoinOperator) nd,
              (TableScanOperator) stack.get(0));
      return null;
    }
  }

  private static void removeSemijoinOptimizationFromSMBJoins(
          OptimizeTezProcContext procCtx) throws SemanticException {
    if (!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION) ||
            procCtx.parseContext.getRsOpToTsOpMap().size() == 0) {
      return;
    }

    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(
            new RuleRegExp("R1", TableScanOperator.getOperatorName() + "%" +
                    ".*" + TezDummyStoreOperator.getOperatorName() + "%" +
                    CommonMergeJoinOperator.getOperatorName() + "%"),
            new SMBJoinOpProc());

    SMBJoinOpProcContext ctx = new SMBJoinOpProcContext();
    // The dispatcher finds SMB and if there is semijoin optimization before it, removes it.
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, ctx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(procCtx.parseContext.getTopOps().values());
    GraphWalker ogw = new PreOrderOnceWalker(disp);
    ogw.startWalking(topNodes, null);

    // Iterate over the map and remove semijoin optimizations if needed.
    for (CommonMergeJoinOperator joinOp : ctx.JoinOpToTsOpMap.keySet()) {
      List<TableScanOperator> tsOps = new ArrayList<TableScanOperator>();
      // Get one top level TS Op directly from the stack
      tsOps.add(ctx.JoinOpToTsOpMap.get(joinOp));

      // Get the other one by examining Join Op
      List<Operator<?>> parents = joinOp.getParentOperators();
      for (Operator<?> parent : parents) {
        if (parent instanceof TezDummyStoreOperator) {
          // already accounted for
          continue;
        }

        assert parent instanceof SelectOperator;
        while(parent != null) {
          if (parent instanceof TableScanOperator) {
            tsOps.add((TableScanOperator) parent);
            break;
          }
          parent = parent.getParentOperators().get(0);
        }
      }

      // Now the relevant TableScanOperators are known, find if there exists
      // a semijoin filter on any of them, if so, remove it.
      ParseContext pctx = procCtx.parseContext;
      for (TableScanOperator ts : tsOps) {
        for (ReduceSinkOperator rs : pctx.getRsOpToTsOpMap().keySet()) {
          if (ts == pctx.getRsOpToTsOpMap().get(rs)) {
            // match!
            GenTezUtils.removeBranch(rs);
            GenTezUtils.removeSemiJoinOperator(pctx, rs, ts);
          }
        }
      }
    }
  }

  private static class SemiJoinCycleRemovalDueTOMapsideJoinContext implements NodeProcessorCtx {
    HashMap<Operator<?>,Operator<?>> childParentMap = new HashMap<Operator<?>,Operator<?>>();
  }

  private static class SemiJoinCycleRemovalDueToMapsideJoins implements NodeProcessor {

    @Override
    public Object process(Node nd, Stack<Node> stack, NodeProcessorCtx procCtx,
                          Object... nodeOutputs) throws SemanticException {

      SemiJoinCycleRemovalDueTOMapsideJoinContext ctx =
              (SemiJoinCycleRemovalDueTOMapsideJoinContext) procCtx;
      ctx.childParentMap.put((Operator<?>)stack.get(stack.size() - 2), (Operator<?>) nd);
      return null;
    }
  }

  private static void removeSemiJoinCyclesDueToMapsideJoins(
          OptimizeTezProcContext procCtx) throws SemanticException {
    if (!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION) ||
            procCtx.parseContext.getRsOpToTsOpMap().size() == 0) {
      return;
    }

    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(
            new RuleRegExp("R1", MapJoinOperator.getOperatorName() + "%" +
                    MapJoinOperator.getOperatorName() + "%"),
            new SemiJoinCycleRemovalDueToMapsideJoins());
    opRules.put(
            new RuleRegExp("R2", MapJoinOperator.getOperatorName() + "%" +
                    CommonMergeJoinOperator.getOperatorName() + "%"),
            new SemiJoinCycleRemovalDueToMapsideJoins());
    opRules.put(
            new RuleRegExp("R3", CommonMergeJoinOperator.getOperatorName() + "%" +
                    MapJoinOperator.getOperatorName() + "%"),
            new SemiJoinCycleRemovalDueToMapsideJoins());
    opRules.put(
            new RuleRegExp("R4", CommonMergeJoinOperator.getOperatorName() + "%" +
                    CommonMergeJoinOperator.getOperatorName() + "%"),
            new SemiJoinCycleRemovalDueToMapsideJoins());

    SemiJoinCycleRemovalDueTOMapsideJoinContext ctx =
            new SemiJoinCycleRemovalDueTOMapsideJoinContext();
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, ctx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(procCtx.parseContext.getTopOps().values());
    GraphWalker ogw = new PreOrderOnceWalker(disp);
    ogw.startWalking(topNodes, null);

    // process the list
    ParseContext pCtx = procCtx.parseContext;
    for (Operator<?> parentJoin : ctx.childParentMap.keySet()) {
      Operator<?> childJoin = ctx.childParentMap.get(parentJoin);

      if (parentJoin.getChildOperators().size() == 1) {
        continue;
      }

      for (Operator<?> child : parentJoin.getChildOperators()) {
        if (!(child instanceof SelectOperator)) {
          continue;
        }

        while(child.getChildOperators().size() > 0) {
          child = child.getChildOperators().get(0);
        }

        if (!(child instanceof ReduceSinkOperator)) {
          continue;
        }

        ReduceSinkOperator rs = ((ReduceSinkOperator) child);
        TableScanOperator ts = pCtx.getRsOpToTsOpMap().get(rs);
        if (ts == null) {
          continue;
        }
        // This is a semijoin branch. Find if this is creating a potential
        // cycle with childJoin.
        for (Operator<?> parent : childJoin.getParentOperators()) {
          if (parent == parentJoin) {
            continue;
          }

          assert parent instanceof ReduceSinkOperator;
          while (parent.getParentOperators().size() > 0) {
            parent = parent.getParentOperators().get(0);
          }

          if (parent == ts) {
            // We have a cycle!
            GenTezUtils.removeBranch(rs);
            GenTezUtils.removeSemiJoinOperator(pCtx, rs, ts);
          }
        }
      }
    }
  }

  private static class SemiJoinRemovalIfNoStatsProc implements NodeProcessor {

    @Override
    public Object process(Node nd, Stack<Node> stack, NodeProcessorCtx procCtx,
                          Object... nodeOutputs) throws SemanticException {
      assert nd instanceof ReduceSinkOperator;
      ReduceSinkOperator rs = (ReduceSinkOperator) nd;
      ParseContext pCtx = ((OptimizeTezProcContext) procCtx).parseContext;
      TableScanOperator ts = pCtx.getRsOpToTsOpMap().get(rs);
      if (ts == null) {
        // nothing to do here.
        return null;
      }

      // This is a semijoin branch. The stack should look like,
      // <Parent Ops>-SEL-GB1-RS1-GB2-RS2
      GroupByOperator gbOp = (GroupByOperator) (stack.get(stack.size() - 2));
      GroupByDesc gbDesc = gbOp.getConf();
      ArrayList<AggregationDesc> aggregationDescs = gbDesc.getAggregators();
      boolean removeSemiJoin = false;
      for (AggregationDesc agg : aggregationDescs) {
        if (agg.getGenericUDAFName() != "bloom_filter") {
          continue;
        }

        GenericUDAFBloomFilterEvaluator udafBloomFilterEvaluator =
                (GenericUDAFBloomFilterEvaluator) agg.getGenericUDAFEvaluator();
        long expectedEntries = udafBloomFilterEvaluator.getExpectedEntries();
        if (expectedEntries == -1 || expectedEntries >
                pCtx.getConf().getLongVar(ConfVars.TEZ_MAX_BLOOM_FILTER_ENTRIES)) {
          removeSemiJoin = true;
          break;
        }
      }

      // Check if big table is big enough that runtime filtering is
      // worth it.
      if (ts.getStatistics() != null) {
        long numRows = ts.getStatistics().getNumRows();
        if (numRows < pCtx.getConf().getLongVar(ConfVars.TEZ_BIGTABLE_MIN_SIZE_SEMIJOIN_REDUCTION)) {
          removeSemiJoin = true;
        }
      }

      if (removeSemiJoin) {
        // The stats are not annotated, remove the semijoin operator
        GenTezUtils.removeBranch(rs);
        GenTezUtils.removeSemiJoinOperator(pCtx, rs, ts);
      }
      return null;
    }
  }

  private void removeSemiJoinIfNoStats(OptimizeTezProcContext procCtx)
          throws SemanticException {
    if(!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION)) {
      // Not needed without semi-join reduction
      return;
    }

    Map<Rule, NodeProcessor> opRules = new LinkedHashMap<Rule, NodeProcessor>();
    opRules.put(
            new RuleRegExp("R1", GroupByOperator.getOperatorName() + "%" +
                    ReduceSinkOperator.getOperatorName() + "%" +
                    GroupByOperator.getOperatorName() + "%" +
                    ReduceSinkOperator.getOperatorName() + "%"),
            new SemiJoinRemovalIfNoStatsProc());
    Dispatcher disp = new DefaultRuleDispatcher(null, opRules, procCtx);
    List<Node> topNodes = new ArrayList<Node>();
    topNodes.addAll(procCtx.parseContext.getTopOps().values());
    GraphWalker ogw = new PreOrderOnceWalker(disp);
    ogw.startWalking(topNodes, null);
  }

  private boolean findParallelSemiJoinBranch(Operator<?> mapjoin, TableScanOperator bigTableTS,
                                             ParseContext parseContext,
                                             Map<ReduceSinkOperator, TableScanOperator> semijoins) {

    boolean parallelEdges = false;
    for (Operator<?> op : mapjoin.getParentOperators()) {
      if (!(op instanceof ReduceSinkOperator)) {
        continue;
      }

      op = op.getParentOperators().get(0);

      // Follow the Reducesink operator upstream which is on small table side.
      while (!(op instanceof ReduceSinkOperator) &&
              !(op instanceof TableScanOperator) &&
              !(op.getChildren() != null && op.getChildren().size() > 1)) {
        op = op.getParentOperators().get(0);
      }

      // Bail out if RS or TS is encountered.
      if (op instanceof ReduceSinkOperator || op instanceof TableScanOperator) {
        continue;
      }

      // A branch is hit.
      for (Node nd : op.getChildren()) {
        if (nd instanceof SelectOperator) {
          Operator<?> child = (Operator<?>) nd;

          while (child.getChildOperators().size() > 0) {
            child = child.getChildOperators().get(0);
          }

          // If not ReduceSink Op, skip
          if (!(child instanceof ReduceSinkOperator)) {
            continue;
          }

          ReduceSinkOperator rs = (ReduceSinkOperator) child;
          TableScanOperator ts = parseContext.getRsOpToTsOpMap().get(rs);
          if (ts == null || ts != bigTableTS) {
            // skip, no semijoin or not the one we are looking for.
            continue;
          }

          // Add the semijoin branch to the map
          semijoins.put(rs, ts);
          parallelEdges = true;
        }
      }
    }
    return parallelEdges;
  }

  /*
   *  The algorithm looks at all the mapjoins in the operator pipeline until
   *  it hits RS Op and for each mapjoin examines if it has paralllel semijoin
   *  edge.
   */
  private void removeSemijoinsParallelToMapJoin(OptimizeTezProcContext procCtx)
          throws SemanticException {
    if(!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION) ||
            !procCtx.conf.getBoolVar(ConfVars.HIVECONVERTJOIN)) {
      // Not needed without semi-join reduction
      return;
    }

    // Get all the TS ops.
    List<Operator<?>> topOps = new ArrayList<>();
    topOps.addAll(procCtx.parseContext.getTopOps().values());

    Map<ReduceSinkOperator, TableScanOperator> semijoins = new HashMap<>();
    for (Operator<?> parent : topOps) {
      // A TS can have multiple branches due to DPP Or Semijoin Opt.
      // USe DFS to traverse all the branches until RS is hit.
      Deque<Operator<?>> deque = new LinkedList<>();
      deque.add(parent);
      while (!deque.isEmpty()) {
        Operator<?> op = deque.poll();
        if (op instanceof ReduceSinkOperator) {
          // Done with this branch
          continue;
        }

        if (op instanceof MapJoinOperator) {
          // A candidate.
          if (!findParallelSemiJoinBranch(op, (TableScanOperator) parent,
                  procCtx.parseContext, semijoins)) {
            // No parallel edge was found for the given mapjoin op,
            // no need to go down further, skip this TS operator pipeline.
            break;
          }
        }
        deque.addAll(op.getChildOperators());
      }
    }

    if (semijoins.size() > 0) {
      for (ReduceSinkOperator rs : semijoins.keySet()) {
        GenTezUtils.removeBranch(rs);
        GenTezUtils.removeSemiJoinOperator(procCtx.parseContext, rs,
                semijoins.get(rs));
      }
    }
  }

  private static boolean canUseNDV(ColStatistics colStats) {
    return (colStats != null) && (colStats.getCountDistint() >= 0);
  }

  private static double getBloomFilterCost(
      SelectOperator sel,
      FilterOperator fil) {
    double cost = -1;
    Statistics selStats = sel.getStatistics();
    if (selStats != null) {
      cost = selStats.getNumRows();

      // Some other things that could be added here to model cost:
      // Cost of computing/sending partial BloomFilter results? BloomFilterSize * # mappers
      // For reduce-side join, add the cost of the semijoin table scan/dependent tablescans?
    }
    return cost;
  }

  private static long getCombinedKeyDomainCardinality(
      ColStatistics selColStat,
      ColStatistics selColSourceStat,
      ColStatistics tsColStat) {
    long keyDomainCardinality = -1;
    if (!canUseNDV(selColStat) || !canUseNDV(tsColStat)) {
      return -1;
    }

    long selColSourceNdv = canUseNDV(selColSourceStat) ? selColSourceStat.getCountDistint() : -1;
    boolean semiJoinKeyIsPK = StatsUtils.inferForeignKey(selColStat, tsColStat);
    if (semiJoinKeyIsPK) {
      // PK/FQ relationship: NDV of selColSourceStat is a superset of what is in tsColStat
      if (selColSourceNdv >= 0) {
        // Most accurate domain cardinality would be source column NDV if available.
        keyDomainCardinality = selColSourceNdv;
      }
    } else {
      if (selColSourceNdv >= 0) {
        // If semijoin keys and ts keys completely unrelated, the cardinality of both sets
        // could be obtained by adding both cardinalities. Would there be an average case?
        keyDomainCardinality = selColSourceNdv + tsColStat.getCountDistint();

        // Don't exceed the range if we have one.
        if (StatsUtils.hasDiscreteRange(selColStat)
            && StatsUtils.hasDiscreteRange(tsColStat)) {
          long range = 0;
          // Trying using the cardinality from the value range.
          ColStatistics.Range combinedRange = StatsUtils.combineRange(selColStat.getRange(), tsColStat.getRange());
          if (combinedRange != null) {
            range = StatsUtils.getRangeDelta(combinedRange);
          } else {
            range = StatsUtils.getRangeDelta(selColStat.getRange())
                + StatsUtils.getRangeDelta(tsColStat.getRange());
          }
          keyDomainCardinality = Math.min(keyDomainCardinality, range);
        }
      }
      // Otherwise, we tried ..
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Computing key domain cardinality, keyDomainCardinality=" + keyDomainCardinality
          + ", semiJoinKeyIsPK=" + semiJoinKeyIsPK
          + ", selColStat=" + selColStat
          + ", selColSourceStat=" + selColSourceStat
          + ", tsColStat=" + tsColStat);
    }

    return keyDomainCardinality;
  }

  private static double getBloomFilterBenefit(
      SelectOperator sel, ExprNodeDesc selExpr,
      FilterOperator fil, ExprNodeDesc tsExpr) {
    double benefit = -1;
    Statistics selStats = sel.getStatistics();
    Statistics filStats = fil.getStatistics();
    if (selStats == null || filStats == null) {
      LOG.debug("No stats available to compute BloomFilter benefit");
      return benefit;
    }

    // For cardinality values use numRows as default, try to use ColStats if available
    long selKeyCardinality = selStats.getNumRows();
    long tsKeyCardinality = filStats.getNumRows();
    long tsRows = filStats.getNumRows();
    long tsRowSize = filStats.getAvgRowSize();
    long keyDomainCardinality = selKeyCardinality + tsKeyCardinality;

    ExprNodeColumnDesc selCol = ExprNodeDescUtils.getColumnExpr(selExpr);
    ExprNodeColumnDesc tsCol = ExprNodeDescUtils.getColumnExpr(tsExpr);
    if (selCol != null && tsCol != null) {
      // Check if there are column stats available for these columns
      ColStatistics selColStat = selStats.getColumnStatisticsFromColName(selCol.getColumn());
      ColStatistics filColStat = filStats.getColumnStatisticsFromColName(tsCol.getColumn());
      if (canUseNDV(selColStat)) {
        selKeyCardinality = selColStat.getCountDistint();
      }
      if (canUseNDV(filColStat)) {
        tsKeyCardinality = filColStat.getCountDistint();
      }
      // Get colstats for the original table column for selCol if possible, this would have
      // more accurate information about the original NDV of the column before any filtering.
      ColStatistics selColSourceStat = null;
      if (selColStat != null) {
        ExprNodeDescUtils.ColumnOrigin selColSource = ExprNodeDescUtils.findColumnOrigin(selCol, sel);
        if (selColSource != null && selColSource.op.getStatistics() != null) {
          selColSourceStat = selColSource.op.getStatistics().getColumnStatisticsFromColName(
              selColSource.col.getColumn());
        }
      }
      long domainCardinalityFromColStats = getCombinedKeyDomainCardinality(
          selColStat, selColSourceStat, filColStat);
      if (domainCardinalityFromColStats >= 0) {
        keyDomainCardinality = domainCardinalityFromColStats;
      }
    }

    // Selectivity: key cardinality of semijoin / domain cardinality
    // Benefit (rows filtered from ts): (1 - selectivity) * # ts rows
    double selectivity = selKeyCardinality / (double) keyDomainCardinality;
    selectivity = Math.min(selectivity, 1);
    benefit = tsRows * (1 - selectivity);

    if (LOG.isDebugEnabled()) {
      LOG.debug("BloomFilter benefit for " + selCol + " to " + tsCol
          + ", selKeyCardinality=" + selKeyCardinality
          + ", tsKeyCardinality=" + tsKeyCardinality
          + ", tsRows=" + tsRows
          + ", keyDomainCardinality=" + keyDomainCardinality);
      LOG.debug("SemiJoin key selectivity=" + selectivity
          + ", benefit=" + benefit);
    }

    return benefit;
  }

  private static double computeBloomFilterNetBenefit(
      SelectOperator sel, ExprNodeDesc selExpr,
      FilterOperator fil, ExprNodeDesc tsExpr) {
    double netBenefit = -1;
    double benefit = getBloomFilterBenefit(sel, selExpr, fil, tsExpr);
    Statistics filStats = fil.getStatistics();
    if (benefit > 0 && filStats != null) {
      double cost = getBloomFilterCost(sel, fil);
      if (cost > 0) {
        long filDataSize = filStats.getNumRows();
        netBenefit = (benefit - cost) / filDataSize;
        LOG.debug("BloomFilter benefit=" + benefit
            + ", cost=" + cost
            + ", tsDataSize=" + filDataSize
            + ", netBenefit=" + (benefit - cost));
      }
    }
    LOG.debug("netBenefit=" + netBenefit);
    return netBenefit;
  }

  private void removeSemijoinOptimizationByBenefit(OptimizeTezProcContext procCtx)
      throws SemanticException {
    if(!procCtx.conf.getBoolVar(ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION)) {
      // Not needed without semi-join reduction
      return;
    }

    List<ReduceSinkOperator> semijoinRsToRemove = new ArrayList<ReduceSinkOperator>();
    Map<ReduceSinkOperator, TableScanOperator> map = procCtx.parseContext.getRsOpToTsOpMap();
    double semijoinReductionThreshold = procCtx.conf.getFloatVar(
        HiveConf.ConfVars.TEZ_DYNAMIC_SEMIJOIN_REDUCTION_THRESHOLD);
    for (ReduceSinkOperator rs : map.keySet()) {
      // rs is semijoin optimization branch, which should look like <Parent>-SEL-GB1-RS1-GB2-RS2
      // Get to the SelectOperator ancestor
      SelectOperator sel = null;
      for (Operator<?> currOp = rs; currOp.getParentOperators().size() > 0; currOp = currOp.getParentOperators().get(0)) {
        if (currOp instanceof SelectOperator) {
          sel = (SelectOperator) currOp;
          break;
        }
      }
      if (sel == null) {
        throw new SemanticException("Unexpected error - could not find SEL ancestor from semijoin branch of " + rs);
      }

      // Check the ndv/rows from the SEL vs the destination tablescan the semijoin opt is going to.
      TableScanOperator ts = map.get(rs);
      RuntimeValuesInfo rti = procCtx.parseContext.getRsToRuntimeValuesInfoMap().get(rs);
      ExprNodeDesc tsExpr = rti.getTsColExpr();
      // In the SEL operator of the semijoin branch, there should be only one column in the operator
      ExprNodeDesc selExpr = sel.getConf().getColList().get(0);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Computing BloomFilter cost/benefit for " + OperatorUtils.getOpNamePretty(rs)
            + " - " + OperatorUtils.getOpNamePretty(ts) + " (" + tsExpr + ")");
      }

      double reductionFactor = computeBloomFilterNetBenefit(sel, selExpr,
              (FilterOperator)ts.getChildOperators().get(0), tsExpr);
      if (reductionFactor < semijoinReductionThreshold) {
        // This semijoin optimization should be removed. Do it after we're done iterating
        semijoinRsToRemove.add(rs);
      }
    }

    for (ReduceSinkOperator rs : semijoinRsToRemove) {
      TableScanOperator ts = map.get(rs);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reduction factor not satisfied for " + OperatorUtils.getOpNamePretty(rs)
            + "-" + OperatorUtils.getOpNamePretty(ts) + ". Removing semijoin optimization.");
      }
      GenTezUtils.removeBranch(rs);
      GenTezUtils.removeSemiJoinOperator(procCtx.parseContext, rs, ts);
    }
  }
}