package io.avaje.http.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 Extendable response class, if this class is extended it will be automatically added to the openapi json file.
 The default status code is 400 but can be changed with for example @StatusCode("404")

 To register this response exception do:
     javalin.exception(BadRequestResponse.class, (badRequestResponse, context) -> {
        context.json(badRequestResponse.getResponse());
        StatusCode statusCode = badRequestResponse.getClass().getAnnotation(StatusCode.class);
        if(statusCode != null)
            context.status(statusCode.value());
        else
            context.status(400);
    });
 */
public class BadRequestResponse extends Exception {
  public final String message;

  protected BadRequestResponse(String message) {
    this.message = message;
  }

  public Map<String, Object> getResponse() {
    Field[] fields = this.getClass().getFields();
    Map<String, Object> map = new LinkedHashMap<>();
    for(Field field: fields){
      if(!Modifier.isPublic(field.getModifiers()))
        continue;

      try {
        map.put(field.getName(), field.get(this));
      }
      catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return map;
  }
}
