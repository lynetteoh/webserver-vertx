package com.moneylion.interview.webserver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  public void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    PropertyConfigurator.configure(MainVerticle.configProperties);
    vertx.deployVerticle(new ServerInitializerVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  /**
   * Start http server
   *
   * @result http server started on port 3000
   */
  @Test
  void start_http_server(Vertx vertx, VertxTestContext testContext) throws Throwable {

    // start server
    vertx.createHttpServer()
      .requestHandler(req -> req.response().end())
      .listen(3000)
      .onComplete(testContext.succeedingThenComplete());

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  /**
   * send GET request to /feature
   *
   * @result Receive a response with canAccess in json body
   */
  @Test
  public void testGetPermission(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create request and send request to API
    client.request(HttpMethod.GET, 3000, "localhost", "/feature?email=xxx@hotmail.com&featureName=add")
      .compose(req -> req.send().compose(HttpClientResponse::body))
      .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
        // verify test result
        Assertions.assertTrue(buffer.toString().contains("canAccess"));
        testContext.completeNow();
      })));
  }

  /**
   * Test GET request to /feature with invalid email format
   *
   * @result Receives a response with 400 status code
   */
  @Test
  public void testInvalidEmail(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create request and send request to API
    client.request(HttpMethod.GET, 3000, "localhost", "/feature?email=xxx&featureName=add")
      .onComplete(req -> req.result().send().onComplete((result) -> {
        // verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(400, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

  /**
   * Test GET request to /feature with invalid featureName
   *
   * @result Receives a response with 400 status code
   */
  @Test
  public void testInvalidFeatureName(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create request and send request to API
    client.request(HttpMethod.GET, 3000, "localhost", "/feature?email=xxx&featureName=123")
      .onComplete(req -> req.result().send().onComplete((result) -> {
        //verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(400, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

  /**
   * Test GET request to /feature with no parameters
   *
   * @result Receives a response with 400 status code
   */
  @Test
  public void testGETWithNoParameters(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create request and send request to API
    client.request(HttpMethod.GET, 3000, "localhost", "/feature")
      .onComplete(req -> req.result().send().onComplete((result) -> {
        //verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(400, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

  /**
   * POST request to /feature with valid json body
   *
   * @Required Document must exists in database
   * @result Receives a response with 304 status code
   */
  @Test
  public void testPostAvailable(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create json payload for request
    String jsonBody = "{\n" +
      "\"featureName\": \"add\",\n" +
      "\"email\": \"xxx@hotmail.com\", \n" +
      "\"enable\": true\n" +
      "}";
    JsonObject json = new JsonObject(jsonBody);

    // create request and send request to API
    client.request(HttpMethod.POST, 3000, "localhost", "/feature")
      .onComplete(req -> req.result().send(json.toBuffer()).onComplete((result) -> {
        //verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(304, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

  /**
   * POST request to /feature with valid json body
   *
   * @Required Document must exists in database
   * @result Receives a response with 200 status code
   */
  @Test
  public void testPostUpdate(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create json body for request
    String jsonBody = "{\n" +
      "\"featureName\": \"add\",\n" +
      "\"email\": \"xxx@hotmail.com\", \n" +
      "\"enable\": false\n" +
      "}";
    JsonObject json = new JsonObject(jsonBody);

    // create request and send request to API
    client.request(HttpMethod.POST, 3000, "localhost", "/feature")
      .onComplete(req -> req.result().send(json.toBuffer()).onComplete((result) -> {
        // verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(200, result.result().statusCode());
        });
        testContext.completeNow();
      }));

    // return to original state
    String newJsonBody = "{\n" +
      "\"featureName\": \"add\",\n" +
      "\"email\": \"xxx@hotmail.com\", \n" +
      "\"enable\": true\n" +
      "}";
    JsonObject newJson = new JsonObject(newJsonBody);
    client.request(HttpMethod.POST, 3000, "localhost", "/feature")
      .onComplete(req -> req.result().send(newJson.toBuffer()).onComplete((result) -> {
        testContext.verify(() -> {
          Assertions.assertEquals(200, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

  /**
   * POST request to /feature with invalid json body
   *
   * @result Receives a response with 400 status code
   */
  @Test
  public void testPostInvalid(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    // create json body for request
    String jsonBody = "{\n" +
      "\"featureName\": \"add\",\n" +
      "\"email\": \"xxxx\", \n" +
      "\"enable\": false\n" +
      "}";
    JsonObject json = new JsonObject(jsonBody);

    // create request and send request to API
    client.request(HttpMethod.POST, 3000, "localhost", "/feature")
      .onComplete(req -> req.result().send(json.toBuffer()).onComplete((result) -> {
        // verify test result
        testContext.verify(() -> {
          Assertions.assertEquals(400, result.result().statusCode());
        });
        testContext.completeNow();
      }));
  }

}
