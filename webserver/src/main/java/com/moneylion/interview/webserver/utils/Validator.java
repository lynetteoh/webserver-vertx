package com.moneylion.interview.webserver.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Validator {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);
  private Schema schema;

  /**
   * Initialize json schema validator
   * @param schemaPath directory for json schema
   */
  public Validator( String schemaPath,  Vertx vertx) {

    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(schemaRouter);

    try {

      JsonObject jsonSchema = new JsonObject(FileReader.fileToString(schemaPath, StandardCharsets.UTF_8));
      schema = parser.parse(jsonSchema);

    } catch (IOException e) {

      logger.error(e.toString());
    }
  }

  /**\
   * validate json with json schema
   * @param json json body
   * @return boolean
   */
  public CompletableFuture<Boolean> validate(JsonObject json) {
    CompletableFuture<Boolean> validateFuture = new CompletableFuture<Boolean>();

    // validate json with schema
    schema.validateAsync(json).onComplete(ar -> {
      if(ar.succeeded())
      {
        validateFuture.complete(true);
      } else {
        validateFuture.completeExceptionally(ar.cause());
      }
    });

    return validateFuture;
  }

  /**
   * validate string is in email format
   * @param email email string
   * @return true if is email. Otherwise, false
   */
  public static boolean validateEmail(String email) {

    return email != null && !email.isEmpty() && (email.contains("@") && email.contains(".com"));
  }

  /**
   * validate string has string only or combination of string and number
   * @param string string to validate
   * @return true if string is string only or combination of string and number. Otherwise, false
   */
  public static boolean validateAlphanumeric(String string) {

    return string != null && !string.isEmpty() && !StringUtils.isNumeric(string);
  }


}
