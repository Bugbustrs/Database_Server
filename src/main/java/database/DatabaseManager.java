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
import java.time.Instant;
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

        httpMeasurement.setHttpResultCode(Integer.parseInt((String) measurementValues.get("code")));
        httpMeasurement.setTimeTakenMs(Double.parseDouble((String) measurementValues.get("time_ms")));

        return Point.measurementByPOJO(HTTPMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(httpMeasurement)
                .build();
    }

    private static Point createDNSPoint(long time, JSONObject measurementValues) {
        DNSLookupMeasurement dnsLookupMeasurement = new DNSLookupMeasurement();

        dnsLookupMeasurement.setHostAddress((String) measurementValues.get("address"));
        dnsLookupMeasurement.setHostName((String) measurementValues.get("real_hostname"));
        dnsLookupMeasurement.setTimeTaken(measurementValues.getDouble("time_ms"));

        return Point.measurementByPOJO(DNSLookupMeasurement.class).time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(dnsLookupMeasurement)
                .build();
    }

    private static Point createPingPoint(long time, JSONObject measurementValues) {
        PingMeasurement pingMeasurement = new PingMeasurement();

        pingMeasurement.setTargetIpAddress((String) measurementValues.get("target_ip"));
        pingMeasurement.setPingMethod((String) measurementValues.get("ping_method"));
        pingMeasurement.setMeanRttMS(Double.parseDouble((String)measurementValues.get("mean_rtt_ms")));
        pingMeasurement.setMaxRttMs(Double.parseDouble((String) measurementValues.get("max_rtt_ms")));
        pingMeasurement.setStddevRttMs(Double.parseDouble((String) measurementValues.get("stddev_rtt_ms")));

        return Point.measurementByPOJO(PingMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(pingMeasurement)
                .build();
    }

    private static Point createTCPPoint(long time, JSONObject measurementValues) {
        TCPMeasurement tcpMeasurement = new TCPMeasurement();

        tcpMeasurement.setSpeedValues((String)measurementValues.get("tcp_speed_results"));
        tcpMeasurement.setDataLimitExceeded(Boolean.parseBoolean((String)measurementValues.get("data_limit_exceeded")));
        tcpMeasurement.setMeasurementDuration(Double.parseDouble((String) measurementValues.get("duration")));

        return Point.measurementByPOJO(TCPMeasurement.class)
                .time(time, TimeUnit.MICROSECONDS)
                .addFieldsFromPOJO(tcpMeasurement)
                .build();
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
        QueryResult queryResult = influxDB.query(new Query("SELECT * FROM "+measurementType, DB_NAME));
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

    private static Timestamp convertStringToTimestamp(String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            // you can change format of date
            Date date = dateFormat.parse(time);
            return new Timestamp(date.getTime());
        } catch (ParseException e) {
            System.out.println("Exception :" + e);
            return null;
        }
    }
}
