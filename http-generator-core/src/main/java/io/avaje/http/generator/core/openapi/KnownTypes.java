package io.avaje.http.generator.core.openapi;

import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper to translate known Java types to OpenAPI Schema.
 */
class KnownTypes {

  interface KnownType {
    Schema<?> createSchema();
  }

  /**
   * Map of known types by canonical name.
   */
  private final Map<String, KnownType> typeMap = new HashMap<>();

  KnownTypes() {
    add(new StringType(), String.class, char[].class, CharSequence.class);
    add(new BoolType(), boolean.class);
    add(new BooleanType(), Boolean.class);
    add(new ShortType(), Short.class);
    add(new PShortType(), short.class);
    add(new IntType(), int.class);
    add(new IntegerType(), Integer.class);
    add(new PLongType(), long.class);
    add(new LongType(), Long.class);

    add(new PNumberType(), double.class, float.class);
    add(new NumberType(), Double.class, Float.class, BigDecimal.class, BigInteger.class);
    add(new DateType(), LocalDate.class, java.sql.Date.class);
    add(new DateTimeType(), Instant.class, OffsetDateTime.class, ZonedDateTime.class, Timestamp.class, java.util.Date.class, LocalDateTime.class);

    add(new UUIDType(), UUID.class);
    add(new URLType(), URL.class);
    add(new URIType(), URI.class);
    add(new FileType(), File.class);
  }

  /**
   * Return the OpenAPI Schema for the given Java type or Null.
   */
  Schema<?> createSchema(String type) {
    final KnownType knownType = typeMap.get(type);
    return (knownType != null) ? knownType.createSchema() : null;
  }

  private void add(KnownType type, Class<?>... keys) {
    for (Class<?> key : keys) {
      typeMap.put(key.getCanonicalName(), type);
    }
  }

  private class FileType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new FileSchema();
    }
  }

  private class StringType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new StringSchema();
    }
  }

  private class BoolType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new BooleanSchema().nullable(Boolean.FALSE);
    }
  }

  private class BooleanType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new BooleanSchema();
    }
  }

  private class ShortType implements KnownType {
    @Override
    public Schema<?> createSchema() { return new IntegerSchema().minimum(new BigDecimal(Short.MIN_VALUE)).maximum(new BigDecimal(Short.MAX_VALUE)); }
  }

  private class PShortType implements KnownType {
    @Override
    public Schema<?> createSchema() { return new IntegerSchema().minimum(new BigDecimal(Short.MIN_VALUE)).maximum(new BigDecimal(Short.MAX_VALUE)).nullable(Boolean.FALSE); }
  }

  private class IntType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new IntegerSchema().nullable(Boolean.FALSE);
    }
  }

  private class IntegerType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new IntegerSchema();
    }
  }

  private class PLongType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new IntegerSchema().format("int64").nullable(Boolean.FALSE);
    }
  }

  private class LongType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new IntegerSchema().format("int64");
    }
  }

  private class PNumberType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new NumberSchema().nullable(Boolean.FALSE);
    }
  }

  private class NumberType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new NumberSchema();
    }
  }

  private class DateType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new DateSchema();
    }
  }

  private class DateTimeType implements KnownType {
    @Override
    public Schema<?> createSchema() {
      return new DateTimeSchema();
    }
  }

  private class UUIDType extends StringBaseType {
    private UUIDType() {
      super("uuid");
    }
  }

  private class URLType extends StringBaseType {
    private URLType() {
      super("url");
    }
  }

  private class URIType extends StringBaseType {
    private URIType() {
      super("uri");
    }
  }

  private class StringBaseType implements KnownType {
    private final String format;

    private StringBaseType(String format) {
      this.format = format;
    }

    @Override
    public Schema<?> createSchema() {
      return new StringSchema().format(format);
    }
  }
}
