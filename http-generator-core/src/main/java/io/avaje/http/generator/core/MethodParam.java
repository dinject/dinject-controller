package io.avaje.http.generator.core;

import javax.lang.model.element.VariableElement;

public class MethodParam extends BaseMethodParam<ElementReader>{
  MethodParam(VariableElement param, String rawType, ProcessingContext ctx, ParamType defaultParamType, boolean formMarker) {
    super(new ElementReader(param, rawType, ctx, defaultParamType, formMarker));
  }

  public void writeCtxGet(Append writer, PathSegments segments) {
    elementParam.writeCtxGet(writer, segments);
  }

  public void addImports(ControllerReader bean) {
    elementParam.addImports(bean);
  }

  public void writeValidate(Append writer) {
    elementParam.writeValidate(writer);
  }

  public void buildParamName(Append writer) {
    elementParam.writeParamName(writer);
  }
}
