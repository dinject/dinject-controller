package io.avaje.http.generator.core.openapi;

import io.avaje.http.generator.core.ElementReader;
import io.swagger.v3.oas.models.media.Schema;

import javax.lang.model.element.Element;

/**
 * Build the OpenAPI for a method parameter.
 */
public class MethodParamDocBuilder extends BaseMethodParamDocBuilder<Element> {

  private final String rawType;

  public MethodParamDocBuilder(MethodDocBuilder methodDoc, ElementReader param) {
    super(
      methodDoc,
      param,
      param.getElement()
    );
    this.rawType = param.getRawType();
  }

  @Override
  protected Schema getSchema() {
    return ctx.toSchema(this.rawType, this.element);
  }
}
