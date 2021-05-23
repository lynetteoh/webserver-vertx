package com.moneylion.interview.webserver;

import io.vertx.core.Vertx;
import org.apache.log4j.PropertyConfigurator;

public class MainVerticle {

  public static final String configProperties = "src/main/resources/logger.properties";

  public static void main(String[] args) {
    // configure logger
    PropertyConfigurator.configure(MainVerticle.configProperties);

    // create http server
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new ServerInitializerVerticle());
  }
}
