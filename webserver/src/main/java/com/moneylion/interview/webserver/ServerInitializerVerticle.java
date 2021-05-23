package com.moneylion.interview.webserver;

import com.moneylion.interview.webserver.utils.DatabaseUtils;
import com.moneylion.interview.webserver.utils.Validator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ServerInitializerVerticle extends AbstractVerticle {

  private DatabaseUtils dbUtils;
  private static final Logger logger = LoggerFactory.getLogger(ServerInitializerVerticle.class);
  private static final String PERMISSIONS = "permissions";

  /**
   * Initialize the server and start it
   */
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // connect to mongodb
    dbUtils = new DatabaseUtils();
    dbUtils.initializeDatabase(vertx, "src/main/resources/mongodb_cloud_config.json", PERMISSIONS);

    // Uncomment this to connect to local mongodb and edit config file
//    dbUtils.initializeDatabase(vertx, "src/main/resources/mongodb_local_config.json", PERMISSIONS);

    // create route for server
    Router router = initializeRoute();

    // start server
    vertx.createHttpServer().requestHandler(router)
      .listen(3000, http -> {

        if (http.succeeded()) {

          startPromise.complete();
          logger.info("HTTP server started on port 3000");

        } else {

          startPromise.fail(http.cause());

        }
      });
  }

  /**
   * Initialize all routes for http server
   *
   * @return router
   */
  public Router initializeRoute() {

    Router router = Router.router(vertx);

    router.route("/feature").handler(BodyHandler.create());
    router.post("/feature").handler(this::validatePostPermission).handler(this::changePermission);
    router.get("/feature").handler(this::validateGetParams).handler(this::getPermission);
    router.route("/").handler(this::getRoot);

    return router;
  }

  /**
   * A handler to validate POST request to /feature.
   * Checks on json body based on provided json schema in src/main/resources/json_schema.json
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void validatePostPermission(RoutingContext routingContext) {

    Validator validator = new Validator("src/main/resources/json_schema.json", vertx);

    // validate json body with schema
    CompletableFuture<Boolean> validate = validator.validate(routingContext.getBodyAsJson());
    validate.whenComplete((validated, exception) -> {

      if (exception != null) {

        logger.error("Validation failed: ", exception);
        routingContext.fail(400, exception);

      } else {

        routingContext.next();

      }
    });
  }

  /**
   * A  handler to validate on the parameters received by GET request for /feature
   * featureName can only be in string or combination of string with number
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void validateGetParams(RoutingContext routingContext) {

    // retrieve request
    HttpServerRequest request = routingContext.request();

    // get parameters
    String email = request.getParam("email");
    String featureName = request.getParam("featureName");

    // validate parameters
    boolean isValidEmail = Validator.validateEmail(email);
    boolean isValidFeatureName = Validator.validateAlphanumeric(featureName);

    if (!isValidEmail || !isValidFeatureName) {

      String error = !isValidEmail ? "email parameter is not in the correct format" : "featureName needs to contain only string or combination of string with numbers.";
      error = !isValidEmail && !isValidFeatureName ? "email and featureName are not in the correct format" : error;
      logger.error("Validation failed for get parameters " + error);

      // send response with Http Status Bad Request
      routingContext.fail(400);

    } else {

      routingContext.next();

    }
  }

  /**
   * A handler to handle to GET request for /
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void getRoot(RoutingContext routingContext) {

    JsonObject json = new JsonObject().put("title", "Vert.x Web");
    createOk(routingContext, json.encodePrettily());
  }

  /**
   * A handler to handle GET request to /feature.
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void getPermission(RoutingContext routingContext) {

    // retrieve request
    HttpServerRequest request = routingContext.request();

    // get parameters
    String email = request.getParam("email");
    String featureName = request.getParam("featureName");

    // create query for database
    JsonObject query = new JsonObject()
                      .put("featureName", featureName)
                      .put("email", email);

    // find document in database
    CompletableFuture<JsonObject> findFuture = dbUtils.findDocument(PERMISSIONS, query);
    logger.info("Finding document in database with email: " + email + " & featureName: " + featureName);
    findFuture.whenComplete((doc, exception) -> {

      // error handling
      if (exception != null) {

        logger.error("Something went wrong while searching for document in database: ", exception);
        routingContext.fail(500, exception);

      } else {

        // document exists in database
        if (doc != null) {

          JsonObject body = new JsonObject().put("canAccess", doc.getMap().get("enable"));
          createOk(routingContext, body.encodePrettily());

        } else {

          // return response with Http Status Not Found
          routingContext.fail(404);

        }
      }
    });
  }

  /**
   * A handler to handle POST request to /feature to add or change user access for a feature
   * Returns an empty response with HTTP Status OK (200) when the database is updated successfully,
   * otherwise returns Http Status Not Modified (304).
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void changePermission(RoutingContext routingContext) {

    // get request body
    JsonObject request = routingContext.getBodyAsJson();

    // create query
    JsonObject query = new JsonObject()
      .put("featureName", request.getValue("featureName"))
      .put("email", request.getValue("email"));

    // update instruction for existing document in database
    JsonObject update = new JsonObject().put("$set", new JsonObject().put("enable", request.getValue("enable")));

    // update document in database if exists. Otherwise, add document to database
    CompletableFuture<MongoClientUpdateResult> updatedPermission = dbUtils.updateDocument(PERMISSIONS, query, update);
    updatedPermission.whenComplete((result, updateException) -> {

      // error handling
      if (updateException != null) {

        logger.error("Something went wrong while updating document in database: ", updateException);

        // return response with Http Status Internal Server Error
        routingContext.fail(500, updateException);

      } else {

        // add to database or modified document
        if (result.getDocModified() != 0 || result.getDocUpsertedId() != null) {

          createOk(routingContext);
          logger.info("Updated document to database");

        } else {

          createNotModified(routingContext);
          logger.info("Document exists in database. No modification required. ");
        }
      }
    });
  }

  /**
   * Create response with Http Status Not Modified (304)
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void createNotModified(RoutingContext routingContext) {

    routingContext.response()
      .setStatusCode(304)
      .setStatusMessage("Not Modified")
      .end();
  }

  /**
   * Create response with Http Status OK (200)
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   */
  private void createOk(RoutingContext routingContext) {

    routingContext.response()
      .setStatusCode(200)
      .setStatusMessage("OK")
      .end();
  }

  /**
   * Create response with Http Status OK (200)
   *
   * @param routingContext Represents the context for the handling of a request in Vert.x-Web
   * @param body           payload for request
   */
  private void createOk(RoutingContext routingContext, String body) {

    routingContext.response()
      .setStatusCode(200)
      .setStatusMessage("OK")
      .end(body);
  }
}
