package benco;

/**
 * Created by Benco on 2015/5/1.
 */
import java.util.ArrayList;
import java.util.logging.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.result.UpdateResult;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.mongodb.MongoClient;
import org.bson.Document;

import javax.print.Doc;

public class DataBase {

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private static MongoCollection<Document> userCollection;

    private Logger logger;
    public DataBase(Logger _logger) {
        logger = _logger;
        init();
    }
    private void init() {
        mongoClient = new MongoClient("localhost", 27017);
        mongoDatabase = mongoClient.getDatabase("wechat");

        userCollection = mongoDatabase.getCollection("user");
    }
    public boolean Register(String nickname , String passwd ) {
        Document doc = new Document("nickname",nickname).append("passwd",passwd);
        try {
            userCollection.insertOne(doc);
        }
        catch (Exception e) {
            return false;
        }
        logger.info(nickname+" "+passwd+"Register successed");
        return true;
    }
    public boolean Login(String nickname , String passwd) {
        String r_passwd;
        try {
            Document doc = userCollection.find(eq("nickname",nickname)).limit(1).first();
            r_passwd = doc.getString("passwd");
        }
        catch (Exception e) {
            return false;
        }
        logger.info("varfiy :"+nickname);
        logger.info("input passwd :"+passwd);
        logger.info("except passwd :"+passwd);
        return passwd.equals(r_passwd);
    }
}
