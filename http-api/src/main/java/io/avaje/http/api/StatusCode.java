package io.avaje.http.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specify the status code for a thrown exception
 *
 * <pre>{@code
 *
 *  @StatusCode("/customers")
 *  class CustomerController extends BadRequestResponse {
 *    ...
 *  }
 *
 * }</pre>
 *
 * <h4>JAX-RS note</h4>
 * <p>
 * It should only be used on exceptions that extend BadRequestResponse
 * Other exceptions won't be added to the swagger documentation
 * </p>
 */

@Target(value=TYPE)
@Retention(value=RUNTIME)
public @interface StatusCode {
  int value();
}
