package io.avaje.http.generator.core;

import io.avaje.http.generator.core.openapi.DocContext;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;

public class ProcessingContext {

  private final PlatformAdapter readAdapter;
  private final Messager messager;
  private final Filer filer;
  private final Elements elements;
  private final Types types;
  private final boolean openApiAvailable;
  private final DocContext docContext;

  ProcessingContext(ProcessingEnvironment env, PlatformAdapter readAdapter) {
    this.readAdapter = readAdapter;
    this.messager = env.getMessager();
    this.filer = env.getFiler();
    this.elements = env.getElementUtils();
    this.types = env.getTypeUtils();
    this.openApiAvailable = isTypeAvailable(Constants.OPENAPIDEFINITION);
    this.docContext = new DocContext(env, openApiAvailable);
  }

  private boolean isTypeAvailable(String canonicalName) {
    return null != getTypeElement(canonicalName);
  }

  public TypeElement getTypeElement(String canonicalName) {
    return elements.getTypeElement(canonicalName);
  }

  public boolean isOpenApiAvailable() {
    return openApiAvailable;
  }

  public void logError(Element e, String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }

  /**
   * Create a file writer for the given class name.
   */
  public JavaFileObject createWriter(String cls, Element origin) throws IOException {
    return filer.createSourceFile(cls, origin);
  }

  public String getDocComment(Element param) {
    return elements.getDocComment(param);
  }

  public DocContext doc() {
    return docContext;
  }

  public Element asElement(TypeMirror typeMirror) {
    return types.asElement(typeMirror);
  }

  public TypeMirror asMemberOf(DeclaredType declaredType, Element element) {
    return types.asMemberOf(declaredType, element);
  }

  public PlatformAdapter platform() {
    return readAdapter;
  }
}
