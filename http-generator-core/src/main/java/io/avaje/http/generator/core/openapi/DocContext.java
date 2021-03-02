package io.avaje.http.generator.core.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.TreeMap;

/**
 * Context for building the OpenAPI documentation.
 */
public class DocContext {

  private final boolean openApiAvailable;

  private final Elements elements;

  private final Filer filer;

  private final Messager messager;

  private final Map<String, PathItem> pathMap = new TreeMap<>();
  private final Map<String, Schema> schemas = new TreeMap<>();

  private final SchemaDocBuilder schemaBuilder;
  private final SchemaDocBuilderClass schemaBuilderClass;

  private final OpenAPI openAPI;

  public DocContext(ProcessingEnvironment env, boolean openApiAvailable) {
    this.openApiAvailable = openApiAvailable;
    this.elements = env.getElementUtils();
    this.filer = env.getFiler();
    this.messager = env.getMessager();
    this.schemaBuilder = new SchemaDocBuilder(env.getTypeUtils(), env.getElementUtils(), schemas);
    this.schemaBuilderClass = new SchemaDocBuilderClass(schemas);
    this.openAPI = initOpenAPI();
  }

  public boolean isOpenApiAvailable() {
    return openApiAvailable;
  }

  private OpenAPI initOpenAPI() {

    OpenAPI openAPI = new OpenAPI();
    openAPI.setPaths(new Paths());

    Info info = new Info();
    info.setTitle("");
    info.setVersion("");
    openAPI.setInfo(info);

    return openAPI;
  }

  Schema toSchema(String rawType, Element element) {
    // TypeElement typeElement = elements.getTypeElement(rawType);
    //  if (typeElement == null) {
    //    primitive types etc
      return schemaBuilder.toSchema(element.asType());
    // } else {
    // return schemaBuilder.toSchema(element.asType());
    // }
  }

  Schema toSchema(Parameter parameter) {
    return schemaBuilderClass.toSchema(parameter.getType(), parameter.getParameterizedType());
  }

  Content createContent(Class returnType, ParameterizedType type, String mediaType) {
    return schemaBuilderClass.createContent(returnType, type, mediaType);
  }

  Content createContent(TypeMirror returnType, String mediaType) {
    return schemaBuilder.createContent(returnType, mediaType);
  }

  PathItem pathItem(String fullPath) {
    return pathMap.computeIfAbsent(fullPath, s -> new PathItem());
  }

  void addFormParam(Operation operation, String varName, Schema schema) {
    schemaBuilder.addFormParam(operation, varName, schema);
  }

  void addRequestBody(Operation operation, Schema schema, boolean asForm, String description) {
    schemaBuilder.addRequestBody(operation, schema, asForm, description);
  }

  /**
   * Return the OpenAPI adding the paths and schemas.
   */
  private OpenAPI getApiForWriting() {

    Paths paths = openAPI.getPaths();
    if (paths == null) {
      paths = new Paths();
      openAPI.setPaths(paths);
    }
    // add paths by natural order
    for (Map.Entry<String, PathItem> entry : pathMap.entrySet()) {
      paths.addPathItem(entry.getKey(), entry.getValue());
    }

    components().setSchemas(schemas);
    return openAPI;
  }

  /**
   * Return the components creating if needed.
   */
  private Components components() {
    Components components = openAPI.getComponents();
    if (components == null) {
      components = new Components();
      openAPI.setComponents(components);
    }
    return components;
  }

  private io.swagger.v3.oas.models.tags.Tag createTagItem(Tag tag){
    io.swagger.v3.oas.models.tags.Tag tagsItem = new io.swagger.v3.oas.models.tags.Tag();
    tagsItem.setName(tag.name());
    tagsItem.setDescription(tag.description());
    // tagsItem.setExtensions(tag.extensions());  # Not sure about the extensions
    // tagsItem.setExternalDocs(tag.externalDocs()); # Not sure about the external docs
    return tagsItem;
  }

  public void addTagsDefinition(Tags tags) {
    if(tags == null)
      return;

    for(Tag tag: tags.value()){
      openAPI.addTagsItem(createTagItem(tag));
    }
  }

  public void addTagsDefinition(Element element) {
    addTagsDefinition(element.getAnnotation(Tags.class));
  }

  public void addTagDefinition(Tag tag) {
    if(tag == null)
      return;

    openAPI.addTagsItem(createTagItem(tag));
  }

  public void addTagDefinition(Element element){
    addTagDefinition(element.getAnnotation(Tag.class));
  }

  public void readApiDefinition(Element element) {

    OpenAPIDefinition openApi = element.getAnnotation(OpenAPIDefinition.class);
    io.swagger.v3.oas.annotations.info.Info info = openApi.info();
    if (!info.title().isEmpty()) {
      openAPI.getInfo().setTitle(info.title());
    }
    if (!info.description().isEmpty()) {
      openAPI.getInfo().setDescription(info.description());
    }
    if (!info.version().isEmpty()) {
      openAPI.getInfo().setVersion(info.version());
    }

  }

  public void writeApi() {
    try (Writer metaWriter = createMetaWriter()) {

      OpenAPI openAPI = getApiForWriting();
      ObjectMapper mapper = createObjectMapper();
      mapper.writeValue(metaWriter, openAPI);

    } catch (IOException e) {
      logError(null, "Error writing openapi file" + e.getMessage());
      e.printStackTrace();
    }
  }

  private ObjectMapper createObjectMapper() {

    ObjectMapper mapper = new ObjectMapper();
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(SerializationFeature.INDENT_OUTPUT);

    return mapper;
  }

  private Writer createMetaWriter() throws IOException {
    FileObject writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "meta", "openapi.json");
    return writer.openWriter();
  }

  private void logError(Element e, String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }
}
