package io.avaje.http.generator.core;

import io.avaje.http.api.Path;
import io.avaje.http.api.Produces;

import java.lang.annotation.Annotation;
import java.util.List;

/*
 B = Either Class or TypeElement, for Bean
 I = Either Class or Element, for interfaces
 M = Either Method or ExecutableElement for interfaceMethods
 */
public abstract class BaseControllerReader<B, I, M> {
  /**
   * The produces media type for the controller. Null implies JSON.
   */
  private final String produces;
  protected final ProcessingContext ctx;
  protected final B beanType;
  protected final List<I> interfaces;
  protected final List<M> interfaceMethods;

  BaseControllerReader(B beanType, ProcessingContext ctx) {
    this.beanType = beanType;
    this.ctx = ctx;
    this.interfaces = initInterfaces();
    this.interfaceMethods = initInterfaceMethods();
    this.produces = initProduces();
  }

  public abstract <A extends Annotation> A findAnnotation(Class<A> type);
  protected abstract List<I> initInterfaces();
  protected abstract List<M> initInterfaceMethods();

  private String initProduces() {
    final Produces produces = findAnnotation(Produces.class);
    return (produces == null) ? null : produces.value();
  }

  public String getPath() {
    Path path = findAnnotation(Path.class);
    if (path == null) {
      return null;
    }
    return Util.trimPath(path.value());
  }

  String getProduces() {
    return produces;
  }
}
