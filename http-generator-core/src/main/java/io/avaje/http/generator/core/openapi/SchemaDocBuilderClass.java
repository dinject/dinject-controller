package io.avaje.http.generator.core.openapi;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.media.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Help build OpenAPI Schema objects.
 */
class SchemaDocBuilderClass {
  private final KnownTypes knownTypes;
  private final Map<String, Schema> schemas;

  SchemaDocBuilderClass(Map<String, Schema> schemas) {
    this.knownTypes = new KnownTypes();
    this.schemas = schemas;
  }

  Schema<?> toSchema(Class theClass, Type theType) {
    Schema<?> schema = knownTypes.createSchema(theClass.getCanonicalName());
    if (schema != null) {
      return schema;
    }

    if(AbstractMap.class.isAssignableFrom(theClass)) {
      return buildMapSchema(theClass, theType);
    }

    if (Collection.class.isAssignableFrom(theClass)) {
      return buildArraySchema(theType);
    }
    return buildObjectSchema(theClass);
  }

  private String getObjectSchemaName(Class type) {
    String canonicalName = type.toString();
    int pos = canonicalName.lastIndexOf('.');
    if (pos > -1) {
      canonicalName = canonicalName.substring(pos + 1);
    }
    return canonicalName;
  }

  private Schema<?> buildObjectSchema(Class object) {
    String objectSchemaKey = getObjectSchemaName(object);

    Schema objectSchema = schemas.get(objectSchemaKey);
    if (objectSchema == null) {
      // Put first to resolve recursive stack overflow
      objectSchema = new ObjectSchema();
      schemas.put(objectSchemaKey, objectSchema);
      populateObjectSchema(object, objectSchema);
    }

    Schema ref = new Schema();
    ref.$ref("#/components/schemas/" + objectSchemaKey);
    return ref;
  }

  private <T> void populateObjectSchema(Class object, Schema<T> objectSchema) {
    for (Field field: allFields(object)) {
      ParameterizedType type = null;
      if (field.getGenericType() instanceof ParameterizedType)
        type = (ParameterizedType) field.getGenericType();

      Schema<?> propSchema = toSchema(field.getType(), type);
      if (isNotNullable(field)) {
        propSchema.setNullable(Boolean.FALSE);
      }
      setLengthMinMax(field, propSchema);
      setFormatFromValidation(field, propSchema);
      objectSchema.addProperties(field.getName(), propSchema);
    }
  }

  private void setFormatFromValidation(Field field, Schema<?> propSchema) {
    if (field.getAnnotation(Email.class) != null) {
      propSchema.setFormat("email");
    }
  }

  Content createContent(Class returnType, ParameterizedType type, String mediaType) {
    MediaType mt = new MediaType();
    mt.setSchema(toSchema(returnType,type));
    Content content = new Content();
    content.addMediaType(mediaType, mt);
    return content;
  }

  private void setLengthMinMax(Field field, Schema<?> propSchema) {
    final Size size = field.getAnnotation(Size.class);
    if (size != null) {
      if (size.min() > 0) {
        propSchema.setMinLength(size.min());
      }
      if (size.max() > 0) {
        propSchema.setMaxLength(size.max());
      }
    }
  }

  /**
   * Gather all the fields (properties) for the given bean element.
   */
  private List<Field> allFields(Class object) {
    List<Field> list = new ArrayList<>();
    gatherProperties(list, object);
    return list;
  }

  /**
   * Recursively gather all the fields (properties) for the given bean element.
   */
  private void gatherProperties(List<Field> fields, Class object) {
    if (object == null) {
      return;
    }
    Class mappedSuper = object.getSuperclass();
    if (mappedSuper != null && !"java.lang.Object".equals(mappedSuper.toString())) {
      gatherProperties(fields, mappedSuper);
    }

    for (Field field : object.getFields()) {
      if (!ignoreField(field)) {
        fields.add(field);
      }
    }
  }

  private Schema<?> buildArraySchema(Type type) {
    ArraySchema arraySchema = new ArraySchema();
    if(type == null || !(type instanceof ParameterizedType))
      return arraySchema;

    Type[] typeArguments = ((ParameterizedType)type).getActualTypeArguments();
    if(typeArguments.length == 0)
      return arraySchema;

    Class parameterClass;
    if (typeArguments[0] instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType)typeArguments[0];
      parameterClass = getClassFromString(parameterizedType.getRawType().getTypeName());
    }
    else {
      parameterClass = getClassFromString(typeArguments[0].getTypeName());
    }

    if(parameterClass == null)
      return arraySchema;

    Schema<?> itemSchema = toSchema(parameterClass, typeArguments[0]);
    arraySchema.setItems(itemSchema);
    return arraySchema;
  }

  private Class getClassFromString(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      System.out.println("Cannot convert " + className + " to class");
      e.printStackTrace();
    }
    return null;
  }

  private Schema<?> buildMapSchema(Class<Map> map, Type type) {
    MapSchema mapSchema = new MapSchema();

    if(type == null || !(type instanceof ParameterizedType))
      return mapSchema;

    Type[] typeArguments = ((ParameterizedType)type).getActualTypeArguments();
    if(typeArguments.length < 2)
      return mapSchema;

    Class parameterClass;
    Type typeArgument = typeArguments[1];
    if (typeArgument instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType)typeArgument;
      parameterClass = getClassFromString(parameterizedType.getRawType().getTypeName());
    }
    else {
      parameterClass = getClassFromString(typeArgument.getTypeName());
    }

    if(parameterClass == null)
      return mapSchema;

    mapSchema.setAdditionalProperties(toSchema(parameterClass, typeArgument));
    return mapSchema;
  }

  /**
   * Ignore static or transient fields.
   */
  private boolean ignoreField(Field field) {
    return isStaticOrTransient(field) || isHiddenField(field);
  }

  private boolean isHiddenField(Field field) {

    Hidden hidden = field.getAnnotation(Hidden.class);
    if (hidden != null) {
      return true;
    }

    for (Annotation annotation : field.getAnnotations()) {
      String simpleName = annotation.annotationType().getSimpleName();
      if ("JsonIgnore".equals(simpleName)) {
        return true;
      }
    }
    return false;
  }

  private boolean isStaticOrTransient(Field field) {
    int modifiers = field.getModifiers();
    return (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers));
  }

  private boolean isNotNullable(Field field) {
    return field.getAnnotation(org.jetbrains.annotations.NotNull.class) != null
      || field.getAnnotation(javax.validation.constraints.NotNull.class) != null;
  }
}
