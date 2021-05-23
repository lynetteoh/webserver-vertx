package com.moneylion.interview.webserver.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DatabaseUtils {

  private MongoClient client;
  private static final Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

  public DatabaseUtils() {}

  /**
   * Connect to mongodb and create database as well as table
   * @param configFile path to configuration file for mongodb
   * @param collections name for table
   */
  public void initializeDatabase(Vertx vertx, String configFile, String... collections) {

    String configText;
    try {
      configText = FileReader.fileToString(configFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error(e.toString());
      throw new RuntimeException(e);
    }

    // connect to mongodb with config
    JsonObject config = new JsonObject(configText);
    client = MongoClient.createShared(vertx, config);

    // retrieve table from database
    // create table if doesn't exist in database
    client.getCollections(asyncResult -> {
      if (asyncResult.failed()) {
        throw new RuntimeException(asyncResult.cause());
      } else {
        List<String> collectionsInDB = asyncResult.result();
        for (String collection : collections) {
          if (!collectionsInDB.contains(collection)) {
            client.createCollection(collection, result -> {
              if (result.failed()) {
                throw new RuntimeException(result.cause());
              } else {
                logger.info("Create table in database: " + collection);
              }
            });
          } else {
            logger.info("Table already exists in database: " + collection);
          }
        }
      }
    });
  }

  public CompletableFuture<MongoClientUpdateResult> updateDocument(String collection, JsonObject query, JsonObject updateDoc) {

    // update options : set upsert to true
    // upsert -> insert document if document doesn't exist
    UpdateOptions options = new UpdateOptions(true);

    CompletableFuture<MongoClientUpdateResult> updateFuture = new CompletableFuture<>();
    client.updateCollectionWithOptions(collection, query, updateDoc, options, res -> {
      if (res.failed()) {
        updateFuture.completeExceptionally(res.cause());
      } else {
        updateFuture.complete(res.result());
      }
    });

    return updateFuture;
  }

  /**
   * Find document in database
   * @param collection table name
   * @param query query for database query
   * @return document if found. Otherwise, null
   */
  public CompletableFuture<JsonObject> findDocument(String collection, JsonObject query) {

    CompletableFuture<JsonObject> findFuture = new CompletableFuture<>();

    client.findOne(collection, query,  null , res -> {
      if (res.succeeded()) {
        findFuture.complete(res.result());
        logger.debug("Found document in database: " + res.result() );
      } else {
        findFuture.completeExceptionally(res.cause());
      }
    });

    return findFuture;
  }

  /**
   * insert document into database
   * @param collection table name
   * @param document document to be placed in database
   * @return true if insert successfully. Otherwise, false
   */
  public CompletableFuture<Boolean> insertDocument(String collection, JsonObject document) {

    CompletableFuture<Boolean> insertFuture = new CompletableFuture<>();

    client.insert(collection, document, res -> {
      if (res.succeeded()) {
        insertFuture.complete(true);
        String id = res.result();
        logger.debug("Inserted document with id: " + id + "\n" + document.toString());
      } else {
        insertFuture.completeExceptionally(res.cause());
      }
    });

    return insertFuture;
  }


}
