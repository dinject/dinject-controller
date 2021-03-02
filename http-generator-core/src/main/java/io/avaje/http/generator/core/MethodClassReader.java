package io.avaje.http.generator.core;

import io.avaje.http.generator.core.javadoc.Javadoc;
import io.avaje.http.generator.core.openapi.MethodDocBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class MethodClassReader extends BaseMethodReader<ControllerClassReader, Method, MethodClassParam> {
  public MethodClassReader(ControllerClassReader controllerClassReader, Method method, ProcessingContext ctx) {
    super(controllerClassReader, method, method.getReturnType().equals(Void.TYPE), ctx);
  }

  @Override
  public Javadoc getJavadoc() {
    return Javadoc.parse("");
  }

  @Override
  public <A extends Annotation> A findAnnotation(Class<A> type) {
    A annotation = element.getAnnotation(type);
    if (annotation != null) {
      return annotation;
    }

    return bean.findMethodAnnotation(type, element);
  }

  @Override
  public List<String> getTags() {
    List<String> tags = new ArrayList<>();
    if (element == null)
      return tags;

    if (element.getAnnotation(Tag.class) != null) {
      tags.add(element.getAnnotation(Tag.class).name());
    }
    if (element.getAnnotation(Tags.class) != null) {
      for (Tag tag : element.getAnnotation(Tags.class).value())
        tags.add(tag.name());
    }

    tags.addAll(bean.getTags());
    return tags;
  }

  public ParameterizedType getReturnType() {
    if(element.getGenericReturnType() instanceof ParameterizedType)
      return (ParameterizedType) element.getGenericReturnType();
    return null;
  }

  public Class<?> getReturnClass() {
    return element.getReturnType();
  }

  void read() {
    for (Parameter parameter: element.getParameters()) {
      params.add(new MethodClassParam(parameter, ctx, defaultParamType(), formMarker));
    }
  }

  public void buildApiDocumentation(ProcessingContext ctx) {
    new MethodDocBuilder(this, ctx.doc()).build();
  }
}
