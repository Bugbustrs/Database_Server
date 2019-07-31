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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private static final String CONFIG_FILE = ".config.json";
    private static JSONObject CONFIGS;

    private static String DB_NAME = "mydb";

    private static InfluxDB influxDB;
    private static MongoDatabase mongoDatabase;
    private static com.mongodb.client.MongoClient mongoClient;
    private static MongoCollection<Document> collection;


    public static boolean init() {
        String text = null;
        try {
            text = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (text == null)
            return false;
        CONFIGS = new JSONObject(text);
        return connectInflux() && connectMongo();


    }

    public static void writeValues(String data) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject values = jsonObject.getJSONObject("values");
        long time = jsonObject.getLong("timestamp");
        Point p;
        switch ((String) jsonObject.get("type")) {
            case TCP_TYPE:
                p = createTCPPoint(time, values);
                break;
            case PING_TYPE:
                p = createPingPoint(time, values);
                break;
            case DNS_TYPE:
                p = createDNSPoint(time, values);
                break;
            case HTTP_TYPE:
                p = createHttpPoint(time, values);
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

    private static Point createTraceRTPoint(long time, JSONObject measurementValues) {
        TracerouteMeasurement tracerouteMeasurement = new TracerouteMeasurement();

        return Point.measurementByPOJO(TracerouteMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(tracerouteMeasurement)
                .build();
    }

    private static Point createHttpPoint(long time, JSONObject measurementValues) {
        HTTPMeasurement httpMeasurement = new HTTPMeasurement();

        httpMeasurement.setHttpResultCode(Integer.parseInt(measurementValues.getString("code")));
        double duration = Double.parseDouble(measurementValues.getString("time_ms"));
        httpMeasurement.setTimeTakenMs(Precision.round(duration, 2));

        return Point.measurementByPOJO(HTTPMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(httpMeasurement)
                .build();
    }

    private static Point createDNSPoint(long time, JSONObject measurementValues) {
        DNSLookupMeasurement dnsLookupMeasurement = new DNSLookupMeasurement();

        dnsLookupMeasurement.setHostAddress(measurementValues.getString("address"));
        dnsLookupMeasurement.setHostName(measurementValues.getString("real_hostname"));
        dnsLookupMeasurement.setTimeTaken(measurementValues.getDouble("time_ms"));

        return Point.measurementByPOJO(DNSLookupMeasurement.class).time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(dnsLookupMeasurement)
                .build();
    }

    private static Point createPingPoint(long time, JSONObject measurementValues) {
        PingMeasurement pingMeasurement = new PingMeasurement();
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

    private static Point createTCPPoint(long time, JSONObject measurementValues) {
        TCPMeasurement tcpMeasurement = new TCPMeasurement();

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
     * @return true if the insert was successful and false otherwise
     */
    public static boolean insertMeasuremtnDetails(String measurementDescr) {
        try {
            Document document = Document.parse(measurementDescr);
            collection.insertOne(document);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static String getMeasurementDetails(String key) {
        Document doc = collection.find(eq(" measurement_description.key", key)).first();
        assert doc != null;
        return doc.toJson();
    }
}
