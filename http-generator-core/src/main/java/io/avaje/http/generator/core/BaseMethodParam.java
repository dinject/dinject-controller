package io.avaje.http.generator.core;

import io.avaje.http.generator.core.openapi.MethodDocBuilder;

public abstract class BaseMethodParam<B extends BaseElementReader> {
  protected final B elementParam;

  BaseMethodParam(B elementParam) {
    this.elementParam = elementParam;
  }

  public boolean isBody() {
    return elementParam.getParamType() == ParamType.BODY;
  }

  public boolean isForm() {
    return elementParam.getParamType() == ParamType.FORM;
  }

  public String getShortType() {
    return elementParam.getShortType();
  }

  public String getName() {
    return elementParam.getVarName();
  }

  public void buildApiDocumentation(MethodDocBuilder methodDoc) {
    elementParam.buildApiDocumentation(methodDoc);
  }
}
