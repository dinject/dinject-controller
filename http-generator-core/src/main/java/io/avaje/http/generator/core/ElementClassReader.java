package io.avaje.http.generator.core;

import io.avaje.http.generator.core.openapi.MethodDocBuilder;
import io.avaje.http.generator.core.openapi.MethodParamClassDocBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class ElementClassReader extends BaseElementReader<Parameter> {

  ElementClassReader(Parameter element, ProcessingContext ctx, ParamType defaultType, boolean formMarker) {
    super(
      element,
      element.getType().getName(),
      element.getName(),
      ctx,
      defaultType,
      formMarker
    );
  }

  @Override
  void buildApiDocumentation(MethodDocBuilder methodDoc) {
    if (!isPlatformContext()) {
      new MethodParamClassDocBuilder(methodDoc, this).build();
    }
  }

  @Override
  public <A extends Annotation> A findAnnotation(Class<A> type) {
    return null;
  }
}
