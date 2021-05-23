# webserver-vertx
 Simple web server with vert.x  - Java

## Prerequisite
1. Java 8 
2. IntelliJ
3. Maven 3.8.1 
4. MongoDB (optional)

### Introduction 
This webserver is build with Java, vert.x framework and MongoDB. Currently, this webserver is connected to MongoDB Atlas. To connect to local mongodb, 
please refer to [MongoDB Configuration](#MongoDBConfiguration). 

#### Run 
1. Command Line 
```
mvn compile 
mvn package
java -cp target/webserver-1.0.0-SNAPSHOT-fat.jar com.moneylion.interview.webserver.MainVerticle
```

2. IntelliJ 
   
   Import project and run from MainVerticle class

### MongoDB Configuration 
1. To connect to local MongoDB, edit the config file: src/main/resources/mongodb_local_config.json. 

2. To connect to your personal cloud MongoDB, edit the config file: src/main/resources/mongodb_cloud_config.json.

### API Supported 
   1.  GET /feature?email=XXX&featureName=XXX
   
This endpoint receives (user’s email) and featureName as request parameters and returns the following response in JSON format. If featureNmae or user do not exists, a response with Http Status Not Found(404) is returned. If parameters received are not in the correct format, a response with Http Status Bad Request(400) is returned. 

```json
{  
	"canAccess": true|false (will be true if the user has access to the featureName
}
```

2.  POST /feature

This endpoint receives the following request in JSON format and returns an empty response with HTTP Status OK (200) when the database is updated successfully, otherwise returns Http Status Not Modified (304). Validation will be perform to request body to make sure it adheres to the json schema. During validation fails, a response with Http Status Bad Request is returned. 

> Request Body: 
```json
{
	"featureName": "xxx", (string)
	"email": "xxx", (string) 
	"enable": true|false (boolean) (uses true to enable a user's access, otherwise, false
}
```


