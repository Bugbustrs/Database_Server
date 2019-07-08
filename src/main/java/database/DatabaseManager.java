package database;

import Measurements.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    /**
     * The different types of measurements we can write to the database
     */
    public static final String TCP_TYPE = "tcpthroughput",
            PING_TYPE = "ping",
            DNS_TYPE = "dns_lookup",
            HTTP_TYPE = "http",
            TRACERT_TYPE = "traceroute";
    private static final String CONFIG_FILE = ".config";
    private static String DB_NAME = "mydb";

    private static InfluxDB influxDB;
    private static String RP_Name="autogen";

    public static void process(String data) {
        JSONObject jsonObject = new JSONObject(data);
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
//                p = createTraceRTPoint(jsonObject);
//                break;
            default:
                p = null;
                break;
        }
        if (p != null) {
            influxDB.write(DB_NAME, "autogen", p);
        }
    }

    private static Point createTraceRTPoint(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(TRACERT_TYPE)
                .time(jsonObject.getLong("timestamp"), TimeUnit.MICROSECONDS)
                .addField("num_hops", (String) measurementValues.get("num_hops"))
                .addField("hop_N_addr_i", String.valueOf(measurementValues.get("hop_N_addr_i")))
                .addField("hop_N_rtt_ms", String.valueOf((List) measurementValues.get("hop_N_rtt_ms")))
                .build();
    }

    private static Point createHttpPoint(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(HTTP_TYPE)
                .time(jsonObject.getLong("timestamp"), TimeUnit.MICROSECONDS)
                .addField("time_ms", (String) measurementValues.get("time_ms"))
                .addField("code ", (String) measurementValues.get("code"))
                .build();
    }

    private static Point createDNSPoint(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(DNS_TYPE)
                .time(jsonObject.getLong("timestamp"), TimeUnit.MICROSECONDS)
                .addField("address", (String) measurementValues.get("address"))
                .addField("real_hostname", (String) measurementValues.get("real_hostname"))
                .addField("time_ms", (String) measurementValues.get("time_ms"))
                .build();
    }

    private static Point createPingPoint(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(PING_TYPE)
                .time(jsonObject.getLong("timestamp"), TimeUnit.MICROSECONDS)
                .addField("target_ip", (String) measurementValues.get("target_ip"))
                .addField("ping_method", (String) measurementValues.get("ping_method"))
                .addField("mean_rtt_ms", (String)measurementValues.get("mean_rtt_ms"))
                .addField("max_rtt_ms", (String) measurementValues.get("max_rtt_ms"))
                .addField("stddev_rtt_ms", (String) measurementValues.get("stddev_rtt_ms"))
                .build();
    }

    private static Point createTCPPoint(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(TCP_TYPE)
                .time(jsonObject.getLong("timestamp"), TimeUnit.MICROSECONDS)
                .addField("tcp_speed_results", String.valueOf(measurementValues.get("tcp_speed_results")))
                .addField("data_limit_exceeded", (String) measurementValues.get("data_limit_exceeded"))
                .addField("duration", (String) measurementValues.get("duration"))
                .build();
    }

    private static Timestamp convertStringToTimestamp(String strDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            // you can change format of date
            Date date = dateFormat.parse(strDate);
            return new Timestamp(date.getTime());
        } catch (ParseException e) {
            System.out.println("Exception :" + e);
            return null;
        }
    }

    public static void connect() {
        String databaseAddress = "", userDetails = "", pwdDetails = "";
        try {
            BufferedReader fileInputStream = new BufferedReader(new FileReader(new File(CONFIG_FILE)));
            databaseAddress = fileInputStream.readLine();
            DB_NAME = fileInputStream.readLine();
            userDetails = fileInputStream.readLine();
            pwdDetails = fileInputStream.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        influxDB = InfluxDBFactory.connect(databaseAddress, userDetails, pwdDetails);
        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            System.out.println("Error pinging server.");
        } else {
            System.out.println(response);
        }
    }

    public static String getMeasurement(String measurementType){
        QueryResult queryResult = influxDB.query(new Query("SELECT * FROM"+measurementType, DB_NAME));
        Gson gsosn = new GsonBuilder().create();
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
        switch (measurementType){
            case TCP_TYPE:
                return gsosn.toJson((List<TCPMeasurement>)resultMapper.toPOJO(queryResult, Measurements.TCPMeasurement.class));
            case PING_TYPE:
                return gsosn.toJson((List<PingMeasurement>)resultMapper.toPOJO(queryResult, PingMeasurement.class));
            case DNS_TYPE:
                return  gsosn.toJson((List<DNSLookupMeasurement>)resultMapper.toPOJO(queryResult, DNSLookupMeasurement.class));
            case HTTP_TYPE:
                return  gsosn.toJson((List<HTTPMeasurement>)resultMapper.toPOJO(queryResult, HTTPMeasurement.class));
            case TRACERT_TYPE:
                return  gsosn.toJson((List<TracerouteMeasurement>)resultMapper.toPOJO(queryResult, TracerouteMeasurement.class));
            default:
                return null;
        }
    }
}