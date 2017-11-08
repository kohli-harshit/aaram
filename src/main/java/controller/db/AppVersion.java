package controller.db;

import com.mongodb.*;
import org.json.JSONObject;

public class AppVersion {

    //Database related fields
    static String hostName = "172.28.15.111";
    static int dbPort = 27017;
    static String dbName = "aaram-db";
    static String collection = "version";
    static String dbQuery = "\"component\":\"APIArt-AARAM\"";

    //This method is used to check if user have latest version of APP or not
    public static void checkVersion(String version) throws Exception {
        ServerAddress serverAddress=new ServerAddress(hostName,dbPort);
        MongoClientOptions mongoClientOptions= MongoClientOptions.builder().serverSelectionTimeout(10000).build();
        try (MongoClient mongoClient = new MongoClient(serverAddress,mongoClientOptions)) {
            DBObject basicDBObject = new BasicDBObject("component","APIArt-AARAM");
            DBCursor dbCursor = mongoClient.getDB(dbName).getCollection(collection).find(basicDBObject);
            if (dbCursor.count() > 1)
                throw new Exception("More than one version of AARAM is present in DB");
            else if (dbCursor.count() == 0)
                throw new Exception("Supported version of AARAM is not present in DB");
            else {
                String dbDoc = dbCursor.next().toString();
                JSONObject jsonObject = new JSONObject(dbDoc);
                String supportedVersion = jsonObject.get("version").toString();
                if (!supportedVersion.equals(version))
                    throw new Exception("You are not on the latest version. Please download it from https://someurl");
            }
        }
    }
}
