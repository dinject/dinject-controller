package io.avaje.http.generator.core;

import io.avaje.http.api.*;
import io.avaje.http.generator.core.openapi.MethodDocBuilder;

import java.lang.annotation.Annotation;

public abstract class BaseElementReader<E> {

  protected final E element;
  protected final String varName;
  protected final String rawType;
  protected final String snakeName;
  protected final ProcessingContext ctx;
  protected final TypeHandler typeHandler;
  protected final String shortType;
  protected final boolean formMarker;
  protected final boolean contextType;

  protected boolean notNullKotlin;
  //protected boolean notNullJavax;

  protected String paramDefault;
  protected ParamType paramType;
  protected String paramName;
  protected boolean impliedParamType;

  BaseElementReader(E element, String rawType, String varName, ProcessingContext ctx,  ParamType defaultType, boolean formMarker) {
    this.element = element;
    this.varName = varName;
    this.paramName = varName;
    this.rawType = rawType;
    this.snakeName = Util.snakeCase(varName);
    this.formMarker = formMarker;
    this.typeHandler = TypeMap.get(rawType);
    this.shortType = Util.shortName(rawType);
    this.ctx = ctx;

    this.contextType = ctx.platform().isContextType(rawType);

    if (!contextType) {
      readAnnotations(defaultType);
    } else {
      paramType = ParamType.CONTEXT;
    }
  }

  /**
   * Build the OpenAPI documentation for this parameter.
   */
  abstract void buildApiDocumentation(MethodDocBuilder methodDoc);

  public abstract <A extends Annotation> A findAnnotation(Class<A> type);

  protected String shortType() {
    if (typeHandler != null) {
      return typeHandler.shortName();
    } else {
      return shortType;
    }
  }

  @Override
  public String toString() {
    return varName + " type:" + rawType + " paramType:" + paramType + " dft:" + paramDefault;
  }

  public String getVarName() {
    return varName;
  }

  public ParamType getParamType() {
    return paramType;
  }

  public String getParamName() { return paramName; }

  public String getShortType() {
    return shortType;
  }

  public String getRawType() {
    return rawType;
  }

  public E getElement() {
    return element;
  }

  private void readAnnotations(ParamType defaultType) {
    notNullKotlin = (findAnnotation(org.jetbrains.annotations.NotNull.class) != null);
    //notNullJavax = (element.getAnnotation(javax.validation.constraints.NotNull.class) != null);

    Default defaultVal = findAnnotation(Default.class);
    if (defaultVal != null) {
      this.paramDefault = defaultVal.value();
    }

    Form form = findAnnotation(Form.class);
    if (form != null) {
      this.paramType = ParamType.FORM;
      return;
    }

    BeanParam beanParam = findAnnotation(BeanParam.class);
    if (beanParam != null) {
      this.paramType = ParamType.BEANPARAM;
      return;
    }

    QueryParam queryParam = findAnnotation(QueryParam.class);
    if (queryParam != null) {
      this.paramName = nameFrom(queryParam.value(), varName);
      this.paramType = ParamType.QUERYPARAM;
      return;
    }

    FormParam formParam = findAnnotation(FormParam.class);
    if (formParam != null) {
      this.paramName = nameFrom(formParam.value(), varName);
      this.paramType = ParamType.FORMPARAM;
      return;
    }

    Cookie cookieParam = findAnnotation(Cookie.class);
    if (cookieParam != null) {
      this.paramName = nameFrom(cookieParam.value(), varName);
      this.paramType = ParamType.COOKIE;
      this.paramDefault = null;
      return;
    }

    Header headerParam = findAnnotation(Header.class);
    if (headerParam != null) {
      this.paramName = nameFrom(headerParam.value(), Util.initcapSnake(snakeName));
      this.paramType = ParamType.HEADER;
      this.paramDefault = null;
      return;
    }

    if (paramType == null) {
      this.impliedParamType = true;
      if (typeHandler != null) {
        // a scalar type that we know how to convert
        this.paramType = defaultType;
      } else {
        this.paramType = formMarker ? ParamType.FORM : ParamType.BODY;
      }
    }
  }

  private String nameFrom(String name, String defaultName) {
    if (name != null && !name.isEmpty()) {
      return name;
    }
    return defaultName;
  }

  protected boolean isPlatformContext() {
    return contextType;
  }
}
