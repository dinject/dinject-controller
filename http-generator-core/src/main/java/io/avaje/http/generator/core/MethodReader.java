package io.avaje.http.generator.core;

import com.sun.tools.javac.code.Symbol;
import io.avaje.http.generator.core.javadoc.Javadoc;
import io.avaje.http.generator.core.openapi.MethodDocBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class MethodReader extends BaseMethodReader<ControllerReader, ExecutableElement, MethodParam> {
  private final Javadoc javadoc;

  /**
   * Holds enum Roles that are required for the method.
   */
  private final List<String> methodRoles;

  private final ExecutableType actualExecutable;
  private final List<? extends TypeMirror> actualParams;

  MethodReader(ControllerReader bean, ExecutableElement element, ExecutableType actualExecutable, ProcessingContext ctx) {
    super(bean, element,element.getReturnType().getKind() == TypeKind.VOID, ctx);
    this.actualExecutable = actualExecutable;
    this.actualParams = (actualExecutable == null) ? null : actualExecutable.getParameterTypes();

    this.methodRoles = Util.findRoles(element);
    this.javadoc = Javadoc.parse(ctx.getDocComment(element));
  }

  public Javadoc getJavadoc() {
    return javadoc;
  }

  public <A extends Annotation> A findAnnotation(Class<A> type) {
    A annotation = element.getAnnotation(type);
    if (annotation != null) {
      return annotation;
    }

    return bean.findMethodAnnotation(type, element);
  }

  private List<String> addTagsToList(Element element, List<String> list) {
    if (element == null)
      return list;

    if (element.getAnnotation(Tag.class) != null) {
      list.add(element.getAnnotation(Tag.class).name());
    }
    if (element.getAnnotation(Tags.class) != null) {
      for (Tag tag : element.getAnnotation(Tags.class).value())
        list.add(tag.name());
    }
    return list;
  }

  public List<String> getTags() {
    List<String> tags = new ArrayList<>();
    tags = addTagsToList(element, tags);
    return addTagsToList(element.getEnclosingElement(), tags);
  }

  void read() {
    if (!methodRoles.isEmpty()) {
      ctx.platform().methodRoles(methodRoles, bean);
    }

    final List<? extends VariableElement> parameters = element.getParameters();
    for (int i = 0; i < parameters.size(); i++) {

      VariableElement p = parameters.get(i);

      String rawType;
      if (actualParams != null) {
        rawType = Util.typeDef(actualParams.get(i));
      } else {
        rawType = Util.typeDef(p.asType());
      }

      MethodParam param = new MethodParam(p, rawType, ctx, defaultParamType(), formMarker);
      params.add(param);
      param.addImports(bean);
    }
  }

  public List<String> roles() {
    return methodRoles.isEmpty() ? bean.getRoles() : methodRoles;
  }

  public String getProduces() {
    return produces;
  }

  public TypeMirror getReturnType() {
    if (actualExecutable != null) {
      return actualExecutable.getReturnType();
    }
    return element.getReturnType();
  }

  public String getStatusCode() {
    return Integer.toString(webMethod.statusCode(isVoid));
  }

  public PathSegments getPathSegments() {
    return pathSegments;
  }

  public boolean includeValidate() {
    return bean.isIncludeValidator() && webMethod != WebMethod.GET;
  }

  public String simpleName() {
    return element.getSimpleName().toString();
  }

  public boolean isFormBody() {
    for (BaseMethodParam param : params) {
      if (param.isForm()) {
        return true;
      }
    }
    return false;
  }

  public String getBodyType() {
    for (BaseMethodParam param : params) {
      if (param.isBody()) {
        return param.getShortType();
      }
    }
    return null;
  }

  public String getBodyName() {
    for (BaseMethodParam param : params) {
      if (param.isBody()) {
        return param.getName();
      }
    }
    return "body";
  }

  public void buildApiDocumentation(ProcessingContext ctx) {
    new MethodDocBuilder(this, ctx.doc()).build();
  }
}
