package io.avaje.http.generator.core;

import io.avaje.http.api.Path;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControllerClassReader extends BaseControllerReader<Class, Class, Method> {
  private final List<MethodClassReader> methods = new ArrayList<>();

  public ControllerClassReader(Class<?> beanType, ProcessingContext ctx) {
    super(beanType, ctx);
  }

  protected List<Method> initInterfaceMethods() {
    List<Method> ifaceMethods = new ArrayList<>();
    for (Class anInterface : interfaces) {
      ifaceMethods.addAll(Arrays.asList(anInterface.getMethods()));
    }
    return ifaceMethods;
  }

  protected List<Class> initInterfaces() {
    List<Class> interfaces = new ArrayList<>();
    for (Class anInterface : beanType.getInterfaces()) {
      if (anInterface.getAnnotation(Path.class) != null) {
        interfaces.add(anInterface);
      }
    }
    return interfaces;
  }

  void read() {
    for (Method method : beanType.getMethods()) {
      readMethod(method);
    }
    readSuper(beanType.getSuperclass());
  }

  /**
   * Read methods from superclasses taking into account generics.
   */
  private void readSuper(Class<?> superClass) {
    if( superClass == null)
      return;
    if ("java.lang.Object".equals(superClass.toString()))
      return;

    for (Method method : superClass.getMethods())
      readMethod(method);
    readSuper(superClass.getSuperclass());
  }

  private void readMethod(Method method) {
    MethodClassReader methodReader = new MethodClassReader(this, method, ctx);
    if (methodReader.isWebMethod()) {
      methodReader.read();
      methods.add(methodReader);
    }
  }

  <A extends Annotation> A findMethodAnnotation(Class<A> type, Method method) {
    for (Method interfaceMethod : interfaceMethods) {
      if (matchMethod(interfaceMethod, method)) {
        final A annotation = interfaceMethod.getAnnotation(type);
        if (annotation != null) {
          return annotation;
        }
      }
    }
    return null;
  }

  private boolean matchMethod(Method interfaceMethod, Method method) {
    return interfaceMethod.toString().equals(method.toString());
  }

  public List<String> getTags() {
    List<String> tags = new ArrayList<>();
    if (beanType == null)
      return tags;

    if (beanType.getAnnotation(Tag.class) != null) {
      tags.add(((Tag)beanType.getAnnotation(Tag.class)).name());
    }
    if (beanType.getAnnotation(Tags.class) != null) {
      for (Tag tag : ((Tags)beanType.getAnnotation(Tags.class)).value())
        tags.add(tag.name());
    }
    return tags;
  }

  @Override
  public <A extends Annotation> A findAnnotation(Class<A> type) {
    Annotation annotation = beanType.getAnnotation(type);
    if (annotation != null) {
      return (A) annotation;
    }
    for (Class anInterface : interfaces) {
      annotation = anInterface.getAnnotation(type);
      if (annotation != null) {
        return (A) annotation;
      }
    }
    return null;
  }

  void buildApiDocumentation(){
    for(MethodClassReader methodClassReader: methods) {
      methodClassReader.buildApiDocumentation(ctx);
    }
  }
}
