package io.avaje.http.generator.core;

import io.avaje.http.api.*;
import io.avaje.http.generator.core.javadoc.Javadoc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

// All methods needed to generate openapi documentation
public abstract class BaseMethodReader<X extends BaseControllerReader,T, P extends BaseMethodParam> {

  protected final ProcessingContext ctx;
  protected final X bean;
  protected final boolean isVoid;
  protected boolean formMarker;
  protected WebMethod webMethod;
  protected String webMethodPath;
  protected final PathSegments pathSegments;
  protected final String produces;
  protected final T element;
  protected List<P> params = new ArrayList<>();

  public abstract Javadoc getJavadoc();
  public abstract List<String> getTags();
  public abstract  <A extends Annotation> A findAnnotation(Class<A> type);
  public abstract void buildApiDocumentation(ProcessingContext ctx);

  public BaseMethodReader(X bean, T element, boolean isVoid, ProcessingContext ctx) {
    this.bean = bean;
    this.element = element;
    this.ctx = ctx;
    this.isVoid = isVoid;

    initWebMethodViaAnnotation();
    if (isWebMethod()) {
      this.pathSegments = PathSegments.parse(Util.combinePath(bean.getPath(), webMethodPath));
    } else {
      this.pathSegments = null;
    }
    this.produces = produces(bean);
  }

  public List<P> getParams() {
    return params;
  }

  private String produces(BaseControllerReader bean) {
    final Produces produces = findAnnotation(Produces.class);
    return (produces != null) ? produces.value() : bean.getProduces();
  }

  private void initWebMethodViaAnnotation() {
    Form form = findAnnotation(Form.class);
    if (form != null) {
      this.formMarker = true;
    }
    Get get = findAnnotation(Get.class);
    if (get != null) {
      initSetWebMethod(WebMethod.GET, get.value());
      return;
    }
    Put put = findAnnotation(Put.class);
    if (put != null) {
      initSetWebMethod(WebMethod.PUT, put.value());
      return;
    }
    Post post = findAnnotation(Post.class);
    if (post != null) {
      initSetWebMethod(WebMethod.POST, post.value());
      return;
    }
    Patch patch = findAnnotation(Patch.class);
    if (patch != null) {
      initSetWebMethod(WebMethod.PATCH, patch.value());
      return;
    }
    Delete delete = findAnnotation(Delete.class);
    if (delete != null) {
      initSetWebMethod(WebMethod.DELETE, delete.value());
    }
  }

  private void initSetWebMethod(WebMethod webMethod, String value) {
    this.webMethod = webMethod;
    this.webMethodPath = value;
  }

  public boolean isWebMethod() { return webMethod != null; }

  public String getFullPath() {
    return pathSegments.fullPath();
  }

  public WebMethod getWebMethod() {
    return webMethod;
  }

  public String getStatusCode() {
    return Integer.toString(webMethod.statusCode(isVoid));
  }

  public boolean isVoid() {
    return isVoid;
  }

  public String getProduces() {
    return produces;
  }

  public void buildApiDoc() {
    buildApiDocumentation(ctx);
  }

  protected ParamType defaultParamType() {
    // non-path parameters default to form or query parameters based on the
    // existence of @Form annotation on the method
    return (formMarker) ? ParamType.FORMPARAM : ParamType.QUERYPARAM;
  }
}
