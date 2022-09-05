// Autogenerated Jamon proxy
// /Users/qudian/Documents/learnjava/hive-branch-2.3/service/src/jamon/org/apache/hive/tmpl/QueryProfileTmpl.jamon

package org.apache.hive.tmpl;

// 23, 1
import java.util.*;
// 24, 1
import org.apache.hadoop.hive.ql.QueryDisplay;
// 25, 1
import org.apache.hive.service.cli.operation.SQLOperationDisplay;

@org.jamon.annotations.Template(
  signature = "BC5430C7B464CDBC61AAAEACAB94EF7B",
  requiredArguments = {
    @org.jamon.annotations.Argument(name = "sod", type = "SQLOperationDisplay")})
public class QueryProfileTmpl
  extends org.jamon.AbstractTemplateProxy
{
  
  public QueryProfileTmpl(org.jamon.TemplateManager p_manager)
  {
     super(p_manager);
  }
  
  public QueryProfileTmpl()
  {
     super("/org/apache/hive/tmpl/QueryProfileTmpl");
  }
  
  protected interface Intf
    extends org.jamon.AbstractTemplateProxy.Intf{
    
    void renderNoFlush(final java.io.Writer jamonWriter) throws java.io.IOException;
    
  }
  public static class ImplData
    extends org.jamon.AbstractTemplateProxy.ImplData
  {
    // 20, 1
    public void setSod(SQLOperationDisplay sod)
    {
      // 20, 1
      m_sod = sod;
    }
    public SQLOperationDisplay getSod()
    {
      return m_sod;
    }
    private SQLOperationDisplay m_sod;
  }
  @Override
  protected ImplData makeImplData()
  {
    return new ImplData();
  }
  @Override @SuppressWarnings("unchecked") public ImplData getImplData()
  {
    return (ImplData) super.getImplData();
  }
  
  
  @Override
  public org.jamon.AbstractTemplateImpl constructImpl(Class<? extends org.jamon.AbstractTemplateImpl> p_class){
    try
    {
      return p_class
        .getConstructor(new Class [] { org.jamon.TemplateManager.class, ImplData.class })
        .newInstance(new Object [] { getTemplateManager(), getImplData()});
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected org.jamon.AbstractTemplateImpl constructImpl(){
    return new QueryProfileTmplImpl(getTemplateManager(), getImplData());
  }
  public org.jamon.Renderer makeRenderer(final SQLOperationDisplay sod)
  {
    return new org.jamon.AbstractRenderer() {
      @Override
      public void renderTo(final java.io.Writer jamonWriter)
        throws java.io.IOException
      {
        render(jamonWriter, sod);
      }
    };
  }
  
  public void render(final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    renderNoFlush(jamonWriter, sod);
    jamonWriter.flush();
  }
  public void renderNoFlush(final java.io.Writer jamonWriter, final SQLOperationDisplay sod)
    throws java.io.IOException
  {
    ImplData implData = getImplData();
    implData.setSod(sod);
    Intf instance = (Intf) getTemplateManager().constructImpl(this);
    instance.renderNoFlush(jamonWriter);
    reset();
  }
  
  
}
