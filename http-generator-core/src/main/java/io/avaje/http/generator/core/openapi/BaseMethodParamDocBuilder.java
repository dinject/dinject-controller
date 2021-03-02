package io.avaje.http.generator.core.openapi;

import io.avaje.http.generator.core.BaseElementReader;
import io.avaje.http.generator.core.ParamType;
import io.avaje.http.generator.core.javadoc.Javadoc;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Build the OpenAPI for a method parameter.
 */
public abstract class BaseMethodParamDocBuilder<T> {

  protected final DocContext ctx;
  private final Javadoc javadoc;
  private final Operation operation;
  private final String paramName;
  private final String varName;

  private final ParamType paramType;
  protected final T element;

  public BaseMethodParamDocBuilder(MethodDocBuilder methodDoc, BaseElementReader param, T element) {
    this.ctx = methodDoc.getContext();
    this.javadoc = methodDoc.getJavadoc();
    this.operation = methodDoc.getOperation();
    this.paramType = param.getParamType();
    this.paramName = param.getParamName();
    this.varName = param.getVarName();
    this.element = element;
  }

  /**
   * Build the OpenAPI documentation for the method parameter.
   */
  public void build() {
    if (paramType == ParamType.FORM || paramType == ParamType.BODY) {
      addMetaRequestBody(ctx, javadoc, operation);
    } else {
      Parameter param = new Parameter();
      param.setName(varName);
      param.setDescription(javadoc.getParams().get(paramName));

      Schema schema = getSchema();
      if (paramType == ParamType.FORMPARAM) {
        ctx.addFormParam(operation, varName, schema);

      } else {
        param.setSchema(schema);
        param.setIn(paramType.getType());
        operation.addParametersItem(param);
      }
    }
  }

  protected abstract Schema getSchema();

  private void addMetaRequestBody(DocContext ctx, Javadoc javadoc, Operation operation) {
    Schema schema = getSchema();
    String description = javadoc.getParams().get(paramName);

    boolean asForm = (paramType == ParamType.FORM);
    ctx.addRequestBody(operation, schema, asForm, description);
  }
}
