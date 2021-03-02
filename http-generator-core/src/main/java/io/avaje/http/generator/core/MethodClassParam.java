package io.avaje.http.generator.core;

import java.lang.reflect.Parameter;

public class MethodClassParam extends BaseMethodParam<ElementClassReader> {
  MethodClassParam(Parameter parameter, ProcessingContext ctx, ParamType defaultType, boolean formMarker) {
    super(
      new ElementClassReader(
        parameter,
        ctx,
        defaultType,
        formMarker
      )
    );
  }
}
