package io.avaje.http.generator.core;

import io.avaje.http.api.Path;
import io.swagger.v3.oas.annotations.Hidden;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the type information for the Controller (bean).
 */
public class ControllerReader extends BaseControllerReader<TypeElement, Element, ExecutableElement>  {
  private final List<String> roles;

  private final List<MethodReader> methods = new ArrayList<>();

  private final Set<String> staticImportTypes = new TreeSet<>();

  private final Set<String> importTypes = new TreeSet<>();

  private final boolean includeValidator;

  /**
   * Flag set when the controller is dependant on a request scope type.
   */
  private boolean requestScope;

  private boolean docHidden;

  ControllerReader(TypeElement beanType, ProcessingContext ctx) {
    super(beanType, ctx);
    this.roles = Util.findRoles(beanType);
    importTypes.add(Constants.GENERATED);
    if (ctx.isOpenApiAvailable()) {
      docHidden = initDocHidden();
    }
    includeValidator = initIncludeValidator();
    importTypes.add(Constants.SINGLETON);
    importTypes.add(Constants.IMPORT_CONTROLLER);
    importTypes.add(beanType.getQualifiedName().toString());
    if (includeValidator) {
      importTypes.add(Constants.VALIDATOR);
    }
  }

  protected List<Element> initInterfaces() {
    List<Element> interfaces = new ArrayList<>();
    for (TypeMirror anInterface : beanType.getInterfaces()) {
      final Element ifaceElement = ctx.asElement(anInterface);
      if (ifaceElement.getAnnotation(Path.class) != null) {
        interfaces.add(ifaceElement);
      }
    }
    return interfaces;
  }

  protected List<ExecutableElement> initInterfaceMethods() {
    List<ExecutableElement> ifaceMethods = new ArrayList<>();
    for (Element anInterface : interfaces) {
      ifaceMethods.addAll(ElementFilter.methodsIn(anInterface.getEnclosedElements()));
    }
    return ifaceMethods;
  }

  public <A extends Annotation> A findAnnotation(Class<A> type) {
    A annotation = beanType.getAnnotation(type);
    if (annotation != null) {
      return annotation;
    }
    for (Element anInterface : interfaces) {
      annotation = anInterface.getAnnotation(type);
      if (annotation != null) {
        return annotation;
      }
    }
    return null;
  }

  <A extends Annotation> A findMethodAnnotation(Class<A> type, ExecutableElement element) {
    for (ExecutableElement interfaceMethod : interfaceMethods) {
      if (matchMethod(interfaceMethod, element)) {
        final A annotation = interfaceMethod.getAnnotation(type);
        if (annotation != null) {
          return annotation;
        }
      }
    }
    return null;
  }

  private boolean matchMethod(ExecutableElement interfaceMethod, ExecutableElement element) {
    return interfaceMethod.toString().equals(element.toString());
  }

  private boolean initDocHidden() {
    return findAnnotation(Hidden.class) != null;
  }

  private boolean initIncludeValidator() {
    return findAnnotation(Valid.class) != null;
  }

  TypeElement getBeanType() {
    return beanType;
  }

  public boolean isDocHidden() {
    return docHidden;
  }

  public boolean isIncludeValidator() {
    return includeValidator;
  }

  /**
   * Return true if the controller has request scoped dependencies.
   * In that case a BeanFactory will have been generated.
   */
  boolean isRequestScoped() {
    return requestScope;
  }

  void read() {
    if (!roles.isEmpty()) {
      ctx.platform().controllerRoles(roles, this);
    }
    for (Element element : beanType.getEnclosedElements()) {
      if (element.getKind() == ElementKind.METHOD) {
        readMethod((ExecutableElement) element);
      } else if (element.getKind() == ElementKind.FIELD) {
        readField(element);
      }
    }
    readSuper(beanType);
  }

  private void readField(Element element) {
    if (!requestScope) {
      final String rawType = element.asType().toString();
      requestScope = RequestScopeTypes.isRequestType(rawType);
    }
  }

  /**
   * Read methods from superclasses taking into account generics.
   */
  private void readSuper(TypeElement beanType) {
    TypeMirror superclass = beanType.getSuperclass();
    if (superclass.getKind() != TypeKind.NONE) {
      DeclaredType declaredType = (DeclaredType) superclass;
      final Element superElement = ctx.asElement(superclass);
      if (!"java.lang.Object".equals(superElement.toString())) {
        for (Element element : superElement.getEnclosedElements()) {
          if (element.getKind() == ElementKind.METHOD) {
            readMethod((ExecutableElement) element, declaredType);
          } else if (element.getKind() == ElementKind.FIELD) {
            readField(element);
          }
        }
        if (superElement instanceof TypeElement) {
          readSuper((TypeElement) superElement);
        }
      }
    }
  }

  private void readMethod(ExecutableElement element) {
    readMethod(element, null);
  }

  private void readMethod(ExecutableElement method, DeclaredType declaredType) {
    ExecutableType actualExecutable = null;
    if (declaredType != null) {
      // actual taking into account generics
      actualExecutable = (ExecutableType) ctx.asMemberOf(declaredType, method);
    }

    MethodReader methodReader = new MethodReader(this, method, actualExecutable, ctx);
    if (methodReader.isWebMethod()) {
      methodReader.read();
      methods.add(methodReader);
    }
  }

  public List<String> getRoles() {
    return roles;
  }

  public List<MethodReader> getMethods() {
    return methods;
  }

  public void addImportType(String rawType) {
    importTypes.add(rawType);
  }

  public void addStaticImportType(String rawType) {
    staticImportTypes.add(rawType);
  }

  public Set<String> getStaticImportTypes() {
    return staticImportTypes;
  }

  public Set<String> getImportTypes() {
    return importTypes;
  }
}
