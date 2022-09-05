// Autogenerated Jamon implementation
// /Users/qudian/Documents/learnjava/hive-branch-2.3/service/src/jamon/org/apache/hive/tmpl/QueryProfileTmpl.jamon

package org.apache.hive.tmpl;

// 23, 1
import java.util.*;
// 24, 1
import org.apache.hadoop.hive.ql.QueryDisplay;
// 25, 1
import org.apache.hive.service.cli.operation.SQLOperationDisplay;

public class QueryProfileTmplImpl
  extends org.jamon.AbstractTemplateImpl
  implements QueryProfileTmpl.Intf

{
  private final SQLOperationDisplay sod;
  protected static QueryProfileTmpl.ImplData __jamon_setOptionalArguments(QueryProfileTmpl.ImplData p_implData)
  {
    return p_implData;
  }
  public QueryProfileTmplImpl(org.jamon.TemplateManager p_templateManager, QueryProfileTmpl.ImplData p_implData)
  {
    super(p_templateManager, __jamon_setOptionalArguments(p_implData));
    sod = p_implData.getSod();
  }
  
  public void renderNoFlush(@SuppressWarnings({"unused","hiding"}) final java.io.Writer jamonWriter)
    throws java.io.IOException
  {
    // 27, 1
    jamonWriter.write("<!--[if IE]>\n<!DOCTYPE html>\n<![endif]-->\n<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\">\n    <title>HiveServer2</title>\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <meta name=\"description\" content=\"\">\n\n    <link href=\"/static/css/bootstrap.min.css\" rel=\"stylesheet\">\n    <link href=\"/static/css/bootstrap-theme.min.css\" rel=\"stylesheet\">\n    <link href=\"/static/css/hive.css\" rel=\"stylesheet\">\n  </head>\n\n  <body>\n  <div class=\"navbar  navbar-fixed-top navbar-default\">\n      <div class=\"container\">\n          <div class=\"navbar-header\">\n              <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\".navbar-collapse\">\n                  <span class=\"icon-bar\"></span>\n                  <span class=\"icon-bar\"></span>\n                  <span class=\"icon-bar\"></span>\n              </button>\n              <a class=\"navbar-brand\" href=\"/hiveserver2.jsp\"><img src=\"/static/hive_logo.jpeg\" alt=\"Hive Logo\"/></a>\n          </div>\n          <div class=\"collapse navbar-collapse\">\n              <ul class=\"nav navbar-nav\">\n                <li class=\"active\"><a href=\"/\">Home</a></li>\n                <li><a href=\"/logs/\">Local logs</a></li>\n                <li><a href=\"/jmx\">Metrics Dump</a></li>\n                <li><a href=\"/conf\">Hive Configuration</a></li>\n                <li><a href=\"/stacks\">Stack Trace</a></li>\n            </ul>\n          </div><!--/.nav-collapse -->\n        </div>\n      </div>\n    </div>\n\n\n    ");
    // 68, 5
    if (sod == null )
    {
      // 68, 24
      jamonWriter.write("\n         <div class=\"jumbotron\">\n           <p>Query not found.  It may have been deleted, increase <i>hive.server2.webui.max.historic.queries</i>\n              to retain more historic query information.</p>\n         </div>\n    ");
    }
    // 73, 5
    else
    {
      // 73, 12
      jamonWriter.write("\n\n\n    <div class=\"container\">\n      <div class=\"row inner_header\">\n        <div class=\"page-header\">\n          <h1>Query Information: ");
      // 79, 34
      org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getQueryDisplay() == null ? "Unknown" : sod.getQueryDisplay().getQueryString()), jamonWriter);
      // 79, 122
      jamonWriter.write("</h1>\n        </div>\n      </div>\n      <div class=\"row\">\n\n\n      <div class=\"tabbable\">\n          <ul class=\"nav nav-pills\">\n              <li class=\"active\"><a href=\"#tab_baseProfile\" data-toggle=\"tab\">Base Profile</a></li>\n              <li class=\"\"><a href=\"#tab_stages\" data-toggle=\"tab\">Stages</a></li>\n              <li class=\"\"><a href=\"#tab_queryPlan\" data-toggle=\"tab\">Query Plan</a></li>\n              <li class=\"\"><a href=\"#tab_perfLogging\" data-toggle=\"tab\">Performance Logging</a></li>\n          </ul>\n          <div class=\"tab-content\" style=\"padding-bottom: 9px; border-bottom: 1px solid #ddd;\">\n              <div class=\"tab-pane active\" id=\"tab_baseProfile\">\n                  ");
      // 94, 19
      {
        // 94, 19
        __jamon_innerUnit__baseProfile(jamonWriter, sod );
      }
      // 94, 47
      jamonWriter.write("\n              </div>\n              <div class=\"tab-pane\" id=\"tab_stages\">\n                  ");
      // 97, 19
      {
        // 97, 19
        __jamon_innerUnit__stages(jamonWriter, sod );
      }
      // 97, 42
      jamonWriter.write("\n              </div>\n              <div class=\"tab-pane\" id=\"tab_queryPlan\">\n                  ");
      // 100, 19
      {
        // 100, 19
        __jamon_innerUnit__queryPlan(jamonWriter, sod );
      }
      // 100, 45
      jamonWriter.write("\n              </div>\n              <div class=\"tab-pane\" id=\"tab_perfLogging\">\n                  ");
      // 103, 19
      {
        // 103, 19
        __jamon_innerUnit__perfLogging(jamonWriter, sod );
      }
      // 103, 47
      jamonWriter.write("\n              </div>\n          </div>\n      </div>\n\n   ");
    }
    // 108, 10
    jamonWriter.write("\n\n</div>\n</div>\n<script src=\"/static/js/jquery.min.js\" type=\"text/javascript\"></script>\n<script src=\"/static/js/bootstrap.min.js\" type=\"text/javascript\"></script>\n<script src=\"/static/js/tab.js\" type=\"text/javascript\"></script>\n</body>\n</html>\n\n</div>\n</div>\n<script src=\"/static/js/jquery.min.js\" type=\"text/javascript\"></script>\n<script src=\"/static/js/bootstrap.min.js\" type=\"text/javascript\"></script>\n<script src=\"/static/js/tab.js\" type=\"text/javascript\"></script>\n</body>\n</html>\n");
  }
  
  
  // 118, 1
  private void __jamon_innerUnit__baseProfile(@SuppressWarnings({"unused","hiding"}) final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    // 122, 5
    jamonWriter.write("<table class=\"table table-striped\">\n        <tr>\n            <td>User Name</td>\n            <td>");
    // 125, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getUserName()), jamonWriter);
    // 125, 40
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Query String</td>\n            <td>");
    // 129, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getQueryDisplay() == null ? "Unknown" : sod.getQueryDisplay().getQueryString()), jamonWriter);
    // 129, 105
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Id</td>\n            <td>");
    // 133, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getQueryDisplay() == null ? "Unknown" : sod.getQueryDisplay().getQueryId()), jamonWriter);
    // 133, 101
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Execution Engine</td>\n            <td>");
    // 137, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getExecutionEngine()), jamonWriter);
    // 137, 47
    jamonWriter.write("\n        </tr>\n        <tr>\n            <td>State</td>\n            <td>");
    // 141, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getState()), jamonWriter);
    // 141, 37
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Opened Timestamp</td>\n            <td>");
    // 145, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(new Date(sod.getBeginTime())), jamonWriter);
    // 145, 51
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Opened (s)</td>\n            <td>");
    // 149, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getElapsedTime()/1000), jamonWriter);
    // 149, 48
    jamonWriter.write("</td>\n        </tr>\n        <tr>\n            <td>Closed Timestamp</td>\n            <td>");
    // 153, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getEndTime() == null ? "Open" : new Date(sod.getEndTime())), jamonWriter);
    // 153, 85
    jamonWriter.write("</td>\n        </tr>\n        ");
    // 155, 9
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getErrorMessage() != null )
    {
      // 155, 97
      jamonWriter.write("\n            <tr>\n                <td>Error</td>\n                <td>");
      // 158, 21
      org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getQueryDisplay().getErrorMessage()), jamonWriter);
      // 158, 66
      jamonWriter.write("</td>\n            </tr>\n        ");
    }
    // 160, 15
    jamonWriter.write("\n        <tr>\n            <td>Latency (s)</td>\n            <td>");
    // 163, 17
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getRuntime() == null ? "Not finished" : sod.getRuntime()/1000), jamonWriter);
    // 163, 88
    jamonWriter.write("</td>\n        </tr>\n    </table>\n");
  }
  
  
  // 168, 1
  private void __jamon_innerUnit__stages(@SuppressWarnings({"unused","hiding"}) final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    // 172, 4
    jamonWriter.write("<table id=\"attributes_table\" class=\"table table-striped\">\n       <tr>\n           <th>Stage Id</th>\n           <th>Status</th>\n           <th>Begin Time</th>\n           <th>End Time</th>\n           <th>Elapsed Time (s)</th>\n           <th>Requires Lock</th>\n           <th>Retry If Fail</th>\n        </tr>\n\n       ");
    // 183, 8
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getTaskDisplays() != null )
    {
      // 183, 96
      jamonWriter.write("\n           ");
      // 184, 12
      for (QueryDisplay.TaskDisplay taskDisplay : sod.getQueryDisplay().getTaskDisplays() )
      {
        // 184, 99
        jamonWriter.write("\n               <tr>\n                   <td>");
        // 186, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.getTaskId() + ":" + taskDisplay.getTaskType()), jamonWriter);
        // 186, 87
        jamonWriter.write("</td>\n                   <td>");
        // 187, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.getStatus()), jamonWriter);
        // 187, 53
        jamonWriter.write("</td>\n                   <td>");
        // 188, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.getBeginTime() == null ? "" : new Date(taskDisplay.getBeginTime())), jamonWriter);
        // 188, 108
        jamonWriter.write("</td>\n                   <td>");
        // 189, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.getEndTime() == null ? "" : new Date(taskDisplay.getEndTime())), jamonWriter);
        // 189, 104
        jamonWriter.write("</td>\n                   <td>");
        // 190, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.getElapsedTime() == null ? "" : taskDisplay.getElapsedTime()/1000), jamonWriter);
        // 190, 107
        jamonWriter.write(" (s) </td>\n                   <td>");
        // 191, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.isRequireLock()), jamonWriter);
        // 191, 57
        jamonWriter.write("</td>\n                   <td>");
        // 192, 24
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(taskDisplay.isRetryIfFail()), jamonWriter);
        // 192, 57
        jamonWriter.write("</td>\n               </tr>\n           ");
      }
      // 194, 19
      jamonWriter.write("\n       ");
    }
    // 195, 14
    jamonWriter.write("\n   </table>\n");
  }
  
  
  // 200, 1
  private void __jamon_innerUnit__queryPlan(@SuppressWarnings({"unused","hiding"}) final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    // 204, 5
    jamonWriter.write("<div class=\"panel panel-default\">\n      <div class=\"panel-heading\">Explain plan</div>\n      <div class=\"panel-body\">\n        <pre>\n        ");
    // 208, 9
    org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(sod.getQueryDisplay() == null ? "Unknown" : sod.getQueryDisplay().getExplainPlan()), jamonWriter);
    // 208, 97
    jamonWriter.write("\n        </pre>\n      </div>\n    </div>\n");
  }
  
  
  // 215, 1
  private void __jamon_innerUnit__perfLogging(@SuppressWarnings({"unused","hiding"}) final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    // 219, 5
    jamonWriter.write("<section>\n      <h3>Compile-time metadata operations</h3>\n        <table id=\"attributes_table\" class=\"table table-striped\">\n          <tr>\n             <th>Call Name</th>\n             <th>Time (ms)</th>\n          </tr>\n\n          ");
    // 227, 11
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getHmsTimings(QueryDisplay.Phase.COMPILATION) != null )
    {
      // 227, 127
      jamonWriter.write("\n             ");
      // 228, 14
      for (Map.Entry<String, Long> time : sod.getQueryDisplay().getHmsTimings(QueryDisplay.Phase.COMPILATION).entrySet() )
      {
        // 228, 132
        jamonWriter.write("\n                 <tr>\n                     <td>");
        // 230, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getKey()), jamonWriter);
        // 230, 45
        jamonWriter.write("</td>\n                     <td>");
        // 231, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getValue()), jamonWriter);
        // 231, 47
        jamonWriter.write("</td>\n                 </tr>\n             ");
      }
      // 233, 21
      jamonWriter.write("\n         ");
    }
    // 234, 16
    jamonWriter.write("\n        </table>\n      </section>\n\n    <section>\n      <h3>Execution-time metadata operations</h3>\n        <table id=\"attributes_table\" class=\"table table-striped\">\n          <tr>\n             <th>Call Name</th>\n             <th>Time (ms)</th>\n          </tr>\n\n          ");
    // 246, 11
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getHmsTimings(QueryDisplay.Phase.EXECUTION) != null )
    {
      // 246, 125
      jamonWriter.write("\n             ");
      // 247, 14
      for (Map.Entry<String, Long> time : sod.getQueryDisplay().getHmsTimings(QueryDisplay.Phase.EXECUTION).entrySet() )
      {
        // 247, 130
        jamonWriter.write("\n                 <tr>\n                     <td>");
        // 249, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getKey()), jamonWriter);
        // 249, 45
        jamonWriter.write("</td>\n                     <td>");
        // 250, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getValue()), jamonWriter);
        // 250, 47
        jamonWriter.write("</td>\n                 </tr>\n             ");
      }
      // 252, 21
      jamonWriter.write("\n         ");
    }
    // 253, 16
    jamonWriter.write("\n        </table>\n      </section>\n\n    <section>\n      <h3>Compile-Time Perf-Logger</h3>\n        <table id=\"attributes_table\" class=\"table table-striped\">\n          <tr>\n             <th>Compile-time Call Name</th>\n             <th>Time (ms)</th>\n          </tr>\n\n          ");
    // 265, 11
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getPerfLogTimes(QueryDisplay.Phase.COMPILATION) != null )
    {
      // 265, 129
      jamonWriter.write("\n             ");
      // 266, 14
      for (Map.Entry<String, Long> time : sod.getQueryDisplay().getPerfLogTimes(QueryDisplay.Phase.COMPILATION).entrySet()  )
      {
        // 266, 135
        jamonWriter.write("\n                 <tr>\n                     <td>");
        // 268, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getKey()), jamonWriter);
        // 268, 45
        jamonWriter.write("</td>\n                     <td>");
        // 269, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getValue()), jamonWriter);
        // 269, 47
        jamonWriter.write("</td>\n                 </tr>\n             ");
      }
      // 271, 21
      jamonWriter.write("\n         ");
    }
    // 272, 16
    jamonWriter.write("\n        </table>\n      </section>\n\n    <section>\n      <h3>Execution-Time Perf-Logger</h3>\n        <table id=\"attributes_table\" class=\"table table-striped\">\n          <tr>\n             <th>Execution-time Call Name</th>\n             <th>Time (ms)</th>\n          </tr>\n\n          ");
    // 284, 11
    if (sod.getQueryDisplay() != null && sod.getQueryDisplay().getPerfLogTimes(QueryDisplay.Phase.EXECUTION) != null )
    {
      // 284, 127
      jamonWriter.write("\n             ");
      // 285, 14
      for (Map.Entry<String, Long> time : sod.getQueryDisplay().getPerfLogTimes(QueryDisplay.Phase.EXECUTION).entrySet()  )
      {
        // 285, 133
        jamonWriter.write("\n                 <tr>\n                     <td>");
        // 287, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getKey()), jamonWriter);
        // 287, 45
        jamonWriter.write("</td>\n                     <td>");
        // 288, 26
        org.jamon.escaping.Escaping.HTML.write(org.jamon.emit.StandardEmitter.valueOf(time.getValue()), jamonWriter);
        // 288, 47
        jamonWriter.write("</td>\n                 </tr>\n             ");
      }
      // 290, 21
      jamonWriter.write("\n         ");
    }
    // 291, 16
    jamonWriter.write("\n        </table>\n      </section>\n");
  }
  
  
}
