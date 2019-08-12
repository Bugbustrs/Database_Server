package database;

import Measurements.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.math3.util.Precision;
import org.bson.Document;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.json.JSONObject;

import java.util.Arrays;
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

    private static InfluxDB influxDB;
    private static MongoDatabase mongoDatabase;
    private static com.mongodb.client.MongoClient mongoClient;
    private static MongoCollection<Document> collection;


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
                //to be consindered later
//                p = createTraceRTPoint(time, values);
//                break;
            default:
                p = null;
                break;
        }
        if (p != null) {
            String RP_Name = "autogen";
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
            String collectionName = CONFIGS.getString("mongoCollName");
            MongoCredential credential = null;
            if (!userDetails.isEmpty()) {
                credential = MongoCredential.createCredential(userDetails, dbName, pwdDetails.toCharArray());
            }
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
            mongoDatabase = mongoClient.getDatabase(dbName);
            collection = mongoDatabase.getCollection(collectionName);
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
    public static String getMeasurement(String measurementType) {
        QueryResult queryResult = influxDB.query(new Query("SELECT * FROM " + measurementType, DB_NAME));
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
            collection.insertOne(document);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getMeasurementDetails(String key) {
        System.out.print("Looking for: " + key);
        Document doc = collection.find(eq(" measurement_description.key", key)).first();
        assert doc != null;
        return doc.toJson();
    }

    //@TODO this should be used to create the measurement object to write to Mongo
    private static Measurements buildMeasurements(JSONObject object, Class<? extends Measurements> T) {
        try {
            Measurements measurements = T.newInstance();
            measurements.setUserName(object.getString("account_name"));
            measurements.setExperiment(object.getBoolean("is_experiment"));
            return measurements;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
