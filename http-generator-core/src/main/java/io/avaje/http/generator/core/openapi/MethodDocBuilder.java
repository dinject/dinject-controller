package io.avaje.http.generator.core.openapi;

import io.avaje.http.api.MediaType;
import io.avaje.http.generator.core.*;
import io.avaje.http.generator.core.javadoc.Javadoc;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.lang.reflect.ParameterizedType;

/**
 * Build the OpenAPI documentation for a method.
 */
public class MethodDocBuilder {
  private final Javadoc javadoc;
  private final BaseMethodReader methodReader;
  private final DocContext ctx;

  private final Operation operation = new Operation();

  public MethodDocBuilder(BaseMethodReader methodReader, DocContext ctx) {
    this.methodReader = methodReader;
    this.ctx = ctx;
    this.javadoc = methodReader.getJavadoc();
  }

  public void build() {

    if (ctx.isOpenApiAvailable() && methodReader.findAnnotation(Hidden.class) != null) {
      return;
    }

    //operation.setOperationId();
    operation.setSummary(javadoc.getSummary());
    operation.setDescription(javadoc.getDescription());
    operation.setTags(methodReader.getTags());

    if (javadoc.isDeprecated()) {
      operation.setDeprecated(true);
    } else if (methodReader.findAnnotation(Deprecated.class) != null) {
      operation.setDeprecated(true);
    }

    PathItem pathItem = ctx.pathItem(methodReader.getFullPath());
    switch (methodReader.getWebMethod()) {
      case GET:
        pathItem.setGet(operation);
        break;
      case PUT:
        pathItem.setPut(operation);
        break;
      case POST:
        pathItem.setPost(operation);
        break;
      case DELETE:
        pathItem.setDelete(operation);
        break;
      case PATCH:
        pathItem.setPatch(operation);
        break;
    }
    if(methodReader instanceof MethodClassReader) {
      for (BaseMethodParam param : ((MethodClassReader) methodReader).getParams()) {
        param.buildApiDocumentation(this);
      }
    }
    else {
      for (BaseMethodParam param : ((MethodReader) methodReader).getParams()) {
        param.buildApiDocumentation(this);
      }
    }

    ApiResponses responses = new ApiResponses();
    operation.setResponses(responses);

    ApiResponse response = new ApiResponse();
    response.setDescription(javadoc.getReturnDescription());

    if (methodReader.isVoid()) {
      if (isEmpty(response.getDescription())) {
        response.setDescription("No content");
      }
    }
    else {
      final String produces = methodReader.getProduces();
      String contentMediaType = (produces == null) ? MediaType.APPLICATION_JSON : produces;
      if(methodReader instanceof MethodClassReader) {
        Class returnClass = ((MethodClassReader) methodReader).getReturnClass();
        ParameterizedType returnType = ((MethodClassReader) methodReader).getReturnType();

        response.setContent(ctx.createContent(returnClass, returnType, contentMediaType));
      }
      else
        response.setContent(ctx.createContent(((MethodReader)methodReader).getReturnType(), contentMediaType));
    }
    responses.addApiResponse(methodReader.getStatusCode(), response);
  }

  DocContext getContext() {
    return ctx;
  }

  Javadoc getJavadoc() {
    return javadoc;
  }

  Operation getOperation() {
    return operation;
  }

  private boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
