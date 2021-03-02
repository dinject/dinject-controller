package io.avaje.http.generator.core.openapi;

import io.avaje.http.generator.core.ElementClassReader;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Parameter;

/**
 * Build the OpenAPI for a method parameter.
 */
public class MethodParamClassDocBuilder extends BaseMethodParamDocBuilder<Parameter> {

  public MethodParamClassDocBuilder(MethodDocBuilder methodDoc, ElementClassReader param) {
    super(
      methodDoc,
      param,
      param.getElement()
    );
  }

  @Override
  protected Schema getSchema() {
    return ctx.toSchema(this.element);
  }
}
