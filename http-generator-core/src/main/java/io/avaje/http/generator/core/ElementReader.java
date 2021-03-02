package io.avaje.http.generator.core;

import io.avaje.http.generator.core.openapi.MethodDocBuilder;
import io.avaje.http.generator.core.openapi.MethodParamDocBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

public class ElementReader extends BaseElementReader<Element> {


  ElementReader(Element element, ProcessingContext ctx, ParamType defaultType, boolean formMarker) {
    this(element, Util.typeDef(element.asType()), ctx, defaultType, formMarker);
  }

  ElementReader(Element element, String rawType, ProcessingContext ctx, ParamType defaultType, boolean formMarker) {
    super(
      element,
      rawType,
      element.getSimpleName().toString(),  // varName
      ctx,
      defaultType,
      formMarker
    );
  }

  private boolean hasParamDefault() {
    return paramDefault != null && !paramDefault.isEmpty();
  }

  private String platformVariable() {
    return ctx.platform().platformVariable(rawType);
  }

  void addImports(ControllerReader bean) {
    if (typeHandler != null) {
      String importType = typeHandler.getImportType();
      if (importType != null) {
        bean.addImportType(rawType);
      }
    } else {
      bean.addImportType(rawType);
    }
  }

  void writeParamName(Append writer) {
    if (isPlatformContext()) {
      writer.append(platformVariable());
    } else {
      writer.append(varName);
    }
  }

  void writeValidate(Append writer) {
    if (!isPlatformContext() && typeHandler == null) {
      writer.append("validator.validate(%s);", varName).eol();
      writer.append("      ");
    }
  }

  void writeCtxGet(Append writer, PathSegments segments) {
    if (isPlatformContext()) {
      // no conversion for this parameter
      return;
    }

    if (paramType == ParamType.BODY && ctx.platform().isBodyMethodParam()) {
      // body passed as method parameter (Helidon)
      return;
    }
    String shortType = shortType();
    writer.append("%s  %s %s = ", ctx.platform().indent(), shortType, varName);
    if (setValue(writer, segments, shortType)) {
      writer.append(";").eol();
    }
  }

  void setValue(Append writer) {
    setValue(writer, PathSegments.EMPTY, shortType());
  }

  @Override
  public <A extends Annotation> A findAnnotation(Class<A> type) {
    return element.getAnnotation(type);
  }

  private boolean setValue(Append writer, PathSegments segments, String shortType) {
//    if (formMarker && impliedParamType && typeHandler == null) {
//      if (ParamType.FORM != paramType) {
//        throw new IllegalStateException("Don't get here?");
//      }
////      // @Form on method and this type is a "bean" so treat is as a form bean
////      writeForm(writer, shortType, varName, ParamType.FORMPARAM);
////      paramType = ParamType.FORM;
////      return false;
//    }
    if (ParamType.FORM == paramType) {
      writeForm(writer, shortType, varName, ParamType.FORMPARAM);
      return false;
    }
    if (ParamType.BEANPARAM == paramType) {
      writeForm(writer, shortType, varName, ParamType.QUERYPARAM);
      return false;
    }
    if (impliedParamType) {
      PathSegments.Segment segment = segments.segment(varName);
      if (segment != null) {
        // path or matrix parameter
        boolean requiredParam = segment.isRequired(varName);
        String asMethod = (typeHandler == null) ? null : (requiredParam) ? typeHandler.asMethod() : typeHandler.toMethod();
        if (asMethod != null) {
          writer.append(asMethod);
        }
        segment.writeGetVal(writer, varName, ctx.platform());
        if (asMethod != null) {
          writer.append(")");
        }
        paramType = ParamType.PATHPARAM;
        return true;
      }
    }

    String asMethod = (typeHandler == null) ? null : typeHandler.toMethod();
    if (asMethod != null) {
      writer.append(asMethod);
    }

    if (typeHandler == null) {
      // this is a body (POST, PATCH)
      writer.append(ctx.platform().bodyAsClass(shortType));

    } else {
      if (hasParamDefault()) {
        ctx.platform().writeReadParameter(writer, paramType, paramName, paramDefault);
      } else {
        boolean checkNull = notNullKotlin || (paramType == ParamType.FORMPARAM && typeHandler.isPrimitive());
        if (checkNull) {
          writer.append("checkNull(");
        }
        ctx.platform().writeReadParameter(writer, paramType, paramName);
        //writer.append("ctx.%s(\"%s\")", paramType, paramName);
        if (checkNull) {
          writer.append(", \"%s\")", paramName);
        }
      }
    }

    if (asMethod != null) {
      writer.append(")");
    }
    return true;
  }

  private void writeForm(Append writer, String shortType, String varName, ParamType defaultParamType) {
    TypeElement formBeanType = ctx.getTypeElement(rawType);
    BeanParamReader form = new BeanParamReader(ctx, formBeanType, varName, shortType, defaultParamType);
    form.write(writer);
  }

  /**
   * Build the OpenAPI documentation for this parameter.
   */
  void buildApiDocumentation(MethodDocBuilder methodDoc) {
    if (!isPlatformContext()) {
      new MethodParamDocBuilder(methodDoc, this).build();
    }
  }
}
