package database;

import Measurements.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.math3.util.Precision;
import org.bson.Document;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.influxdb.impl.InfluxDBResultMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseManager {
    /**
     * The different types of measurements we can write to the database
     */
    private static final String TCP_TYPE = "tcpthroughput",
            PING_TYPE = "ping",
            DNS_TYPE = "dns_lookup",
            HTTP_TYPE = "http",
            TRACERT_TYPE = "traceroute";
    private static JSONObject CONFIGS;

    private static String DB_NAME = "mydb";
    private static String RP_Name = "autogen";
    private static InfluxDB influxDB;
    private static MongoCollection<Document> jobData, users, researchers, personalData, tempData;


    public static boolean init(JSONObject config) {
        CONFIGS = config;
        return connectInflux() && connectMongo();
    }

    public static void writeValues(JSONObject jsonObject) {
        System.out.println(jsonObject.toString());
        Point p;
        switch ((String) jsonObject.get("type")) {
            case TCP_TYPE:
                p = createTCPPoint(jsonObject);
                break;
            case PING_TYPE:
                p = createPingPoint(jsonObject);
                break;
            case DNS_TYPE:
                p = createDNSPoint(jsonObject);
                break;
            case HTTP_TYPE:
                p = createHttpPoint(jsonObject);
                break;
            case TRACERT_TYPE:
                try {
                    System.out.println(jsonObject);
                    Document document = Document.parse(jsonObject.toString());
                    tempData.insertOne(document);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            default:
                p = null;
                break;
        }
        if (p != null) {
            influxDB.write(DB_NAME, RP_Name, p);
        }
    }

    private static Point createTraceRTPoint(JSONObject jsonObject) {
        JSONObject measurementValues = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");

        TracerouteMeasurement tracerouteMeasurement = (TracerouteMeasurement) buildMeasurements(jsonObject, TracerouteMeasurement.class);

        return Point.measurementByPOJO(TracerouteMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(tracerouteMeasurement)
                .build();
    }

    private static Point createHttpPoint(JSONObject jsonObject) {
        JSONObject measurementValues = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");

        HTTPMeasurement httpMeasurement = (HTTPMeasurement) buildMeasurements(jsonObject, HTTPMeasurement.class);

        httpMeasurement.setHttpResultCode(Integer.parseInt(measurementValues.getString("code")));
        double duration = Double.parseDouble(measurementValues.getString("time_ms"));
        httpMeasurement.setTimeTakenMs(Precision.round(duration, 2));

        return Point.measurementByPOJO(HTTPMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(httpMeasurement)
                .build();
    }

    private static Point createDNSPoint(JSONObject jsonObject) {
        JSONObject measurementValues = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");

        DNSLookupMeasurement dnsLookupMeasurement = (DNSLookupMeasurement) buildMeasurements(jsonObject, DNSLookupMeasurement.class);

        dnsLookupMeasurement.setHostAddress(measurementValues.getString("address"));
        dnsLookupMeasurement.setHostName(measurementValues.getString("real_hostname"));
        dnsLookupMeasurement.setTimeTaken(measurementValues.getDouble("time_ms"));

        return Point.measurementByPOJO(DNSLookupMeasurement.class).time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(dnsLookupMeasurement)
                .build();
    }

    private static Point createPingPoint(JSONObject jsonObject) {
        JSONObject measurementValues = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");

        PingMeasurement pingMeasurement = (PingMeasurement) buildMeasurements(jsonObject, PingMeasurement.class);
        double mean, max, std;

        mean = Double.parseDouble(measurementValues.getString("mean_rtt_ms"));
        max = Double.parseDouble(measurementValues.getString("max_rtt_ms"));
        std = Double.parseDouble(measurementValues.getString("stddev_rtt_ms"));

        pingMeasurement.setTargetIpAddress(measurementValues.getString("target_ip"));
        pingMeasurement.setPingMethod(measurementValues.getString("ping_method"));
        pingMeasurement.setMeanRttMS(Precision.round(mean, 2));
        pingMeasurement.setMaxRttMs(Precision.round(max, 2));
        pingMeasurement.setStddevRttMs(Precision.round(std, 2));

        return Point.measurementByPOJO(PingMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(pingMeasurement)
                .build();
    }

    private static Point createTCPPoint(JSONObject jsonObject) {
        JSONObject measurementValues = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");
        TCPMeasurement tcpMeasurement = (TCPMeasurement) buildMeasurements(jsonObject, TCPMeasurement.class);

        tcpMeasurement.setSpeedValues(measurementValues.getString("tcp_speed_results"));
        tcpMeasurement.setDataLimitExceeded(Boolean.parseBoolean(measurementValues.getString("data_limit_exceeded")));
        double duration = Double.parseDouble(measurementValues.getString("duration"));
        tcpMeasurement.setMeasurementDuration(Precision.round(duration, 2));
        return Point.measurementByPOJO(TCPMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(tcpMeasurement)
                .build();
    }

    public static boolean connectInflux() {
        String databaseAddress = CONFIGS.getString("influxDBAdd");
        DB_NAME = CONFIGS.getString("influxDBName");
        String userDetails = CONFIGS.getString("influxDBUser");
        String pwdDetails = CONFIGS.getString("influxDBPassword");
        influxDB = InfluxDBFactory.connect(databaseAddress, userDetails, pwdDetails);
        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            System.err.println("Error Connecting to Influx DB");
            return false;
        } else {
            System.out.println(response);
            return true;
        }
    }

    /**
     * Attempts to connects to a mongo database using the details in the given config files
     *
     * @return true if the connection was successfull and false otherwise
     */
    public static boolean connectMongo() {
        try {
            String databaseAddress = CONFIGS.getString("mongoDBAdd");
            String dbName = CONFIGS.getString("mongoDBName");
            String userDetails = CONFIGS.getString("mongoDBUser");
            String pwdDetails = CONFIGS.getString("mongoDBPassword");
            MongoCredential credential = null;
            if (!userDetails.isEmpty()) {
                credential = MongoCredential.createCredential(userDetails, dbName, pwdDetails.toCharArray());
            }
            com.mongodb.client.MongoClient mongoClient;
            if (databaseAddress.isEmpty()) {
                if (credential == null)
                    mongoClient = MongoClients.create();
                else {
                    mongoClient = MongoClients.create(
                            MongoClientSettings.builder()
                                    .applyToClusterSettings(builder ->
                                            builder.hosts(Arrays.asList(new ServerAddress())))
                                    .credential(credential)
                                    .build());
                }
            } else {
                if (credential == null) {
                    mongoClient = MongoClients.create(databaseAddress);
                } else {
                    mongoClient = MongoClients.create(
                            MongoClientSettings.builder()
                                    .applyToClusterSettings(builder ->
                                            builder.hosts(Arrays.asList(new ServerAddress(databaseAddress))))
                                    .credential(credential)
                                    .build());
                }
            }
            MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);
            jobData = mongoDatabase.getCollection(CONFIGS.getString("job_data"));
            users = mongoDatabase.getCollection(CONFIGS.getString("basic_users"));
            personalData = mongoDatabase.getCollection(CONFIGS.getString("personal"));
            tempData = mongoDatabase.getCollection(CONFIGS.getString("temp"));
//            adminUsers = mongoDatabase.getCollection(CONFIGS.getString("basic_users"));
            researchers = mongoDatabase.getCollection(CONFIGS.getString("researchers"));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Reads the database to see if we have measurements of this type. Returns all the measurements of the same type as String
     *
     * @param measurementType the type of measurement we want to return
     * @return String representation of the JSon representing.
     */
    public static String getMeasurement(String measurementType, String jobId) {
        QueryResult queryResult;
        if (jobId == null || jobId.isEmpty())
            queryResult = influxDB.query(new Query("SELECT * FROM " + measurementType, DB_NAME));
        else
            queryResult = influxDB.query(new Query("SELECT * FROM  WHERE taskKey=" + jobId + measurementType, DB_NAME));
        Gson gsosn = new GsonBuilder().create();
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
        switch (measurementType) {
            case TCP_TYPE:
                return gsosn.toJson(resultMapper.toPOJO(queryResult, TCPMeasurement.class));
            case PING_TYPE:
                return gsosn.toJson(resultMapper.toPOJO(queryResult, PingMeasurement.class));
            case DNS_TYPE:
                return gsosn.toJson(resultMapper.toPOJO(queryResult, DNSLookupMeasurement.class));
            case HTTP_TYPE:
                return gsosn.toJson(resultMapper.toPOJO(queryResult, HTTPMeasurement.class));
            case TRACERT_TYPE:
                return gsosn.toJson(resultMapper.toPOJO(queryResult, TracerouteMeasurement.class));
            default:
                return null;
        }
    }

    /**
     * Attempts to write Measurement Details into the database use to write the data.
     *
     * @param measurementDescr the data that we are wering
     */
    public static void insertMeasurementDetails(String measurementDescr) {
        try {
            System.out.println(measurementDescr);
            Document document = Document.parse(measurementDescr);
            jobData.insertOne(document);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getMeasurementOfTypes(String type) {
        System.out.print("Looking for: " + type);
        FindIterable<Document> doc = jobData.find(eq("job_description.measurement_description.type", type));
        return convertIterable(doc);
    }

    public static String getMeasurementDetails(String key) {
        System.out.print("Looking for: " + key);
        Document doc = jobData.find(eq("job_description.measurement_description.key", key)).first();
        assert doc != null;
        return doc.toJson();
    }

    public static String getUserJobs(String userID) {
        System.out.print("Looking for data for: " + userID);
        FindIterable<Document> doc = jobData.find(eq(" user", hashUserName(userID)));
        return convertIterable(doc);
    }

    private static String convertIterable(FindIterable<Document> f) {
        JSONArray jobs = new JSONArray();
        for (Document d : f) {
            jobs.put(new JSONObject(d.toJson()));
        }
        System.out.println("Resulting array is " + jobs);
        return jobs.toString();
    }

    public static boolean isUserContained(String userId, String type) {
        System.out.print("Looking for: " + userId);
        Document doc;
        doc = users.find(eq(" user_name", userId)).first();
        return doc != null && doc.getString("user_type").equals(type);
    }

    private static String getTargetKey(JSONObject object, String type) {
        switch (type) {
            case TCP_TYPE:
            case TRACERT_TYPE:
            case DNS_TYPE:
                return object.getString("target");
            case PING_TYPE:
                return object.getString("target_ip");
            case HTTP_TYPE:
                return object.getString("url");
            default:
                return "";
        }
    }

    private static Measurements buildMeasurements(JSONObject object, Class<? extends Measurements> T) {
        try {
            Measurements measurements = T.newInstance();
            String user = object.getString("account_name");
            measurements.setUserName(hashUserName(user));
            measurements.setExperiment(object.getBoolean("is_experiment"));
            measurements.setTarget(getTargetKey(object.getJSONObject("parameters"), object.getString("type")));
            if (measurements.getIsExperiment())
                measurements.setTaskKey(object.getString("task_key"));
            return measurements;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writePcapData(List<PcapMeasurements> pcapMeasurements) {
        BatchPoints batchPoints = BatchPoints
                .database(DB_NAME)
                .tag("async", "true")
                .retentionPolicy(RP_Name)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        for (PcapMeasurements p : pcapMeasurements) {
            batchPoints.point(Point.measurementByPOJO(PcapMeasurements.class)
                    .time(p.getTime().toEpochMilli(), TimeUnit.MICROSECONDS)
                    .build());
        }
        influxDB.write(batchPoints);
    }

    public static void writePersonalData(String data) {
        try {
            System.out.println(data);
            JSONObject object = new JSONObject(data);
            object.put("user_name", hashUserName(object.getString("user_name")));
            Document document = Document.parse(object.toString());
            personalData.insertOne(document);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String readPersonalData(String username, long startTimeMillis, long endTimeMillis) {
        username = hashUserName(username);
        BasicDBObject query = new BasicDBObject("user_name", username)
                .append("date", new BasicDBObject("$gte", startTimeMillis).append("$lte", endTimeMillis));
        FindIterable<Document> results = personalData.find(query);
        return results.toString();
    }

    private static String hashUserName(String userName) {
        if (userName.equals("Anonymous")) {
            return userName;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hashInBytes = md.digest(userName.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
