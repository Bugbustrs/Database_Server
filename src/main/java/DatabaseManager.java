import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class DatabaseManager {
    /**
     * The different types of measurements we can write to the database
     */
    private static final String TCP_TYPE = "tcpthroughput",
            PING_TYPE = "ping",
            DNS_TYPE = "dns_lookup",
            HTTP_TYPE = "http",
            TRACERT_TYPE = "traceroute";
    private static final String CONFIG_FILE = ".config";
    private static  String DB_NAME = "mydb";

    private static InfluxDB influxDB;

    static void process(String data) {
        JSONObject jsonObject = new JSONObject(data);
        Point p;
        switch ((String) jsonObject.get("type")) {
            case TCP_TYPE:
                p = writeTCPDataToDB(jsonObject);
                break;
            case PING_TYPE:
                p = writePingDataToDB(jsonObject);
                break;
            case DNS_TYPE:
                p = writeDNSDataToDB(jsonObject);
                break;
            case HTTP_TYPE:
                p = writeHTTPDataToDB(jsonObject);
                break;
            case TRACERT_TYPE:
                p = writeTracertDataToDB(jsonObject);
                break;
            default:
                p = null;
                break;
        }
        if (p != null) {
            influxDB.write(DB_NAME, "autogen", p);
        }
    }

    private static Point writeTracertDataToDB(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(PING_TYPE)
                .time(Objects.requireNonNull(convertStringToTimestamp((String) jsonObject.get("timestamp"))).getTime(), TimeUnit.MILLISECONDS)
                .addField("num_hops", (Integer) measurementValues.get("num_hops"))
                .addField("ping_method ", String.valueOf(measurementValues.get("hop_N_addr_i")))
                .addField("hop_N_rtt_ms", (Double) measurementValues.get("hop_N_rtt_ms"))
                .build();
    }

    private static Point writeHTTPDataToDB(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(HTTP_TYPE)
                .time(Objects.requireNonNull(convertStringToTimestamp((String) jsonObject.get("timestamp"))).getTime(), TimeUnit.MILLISECONDS)
                .addField("time_ms", (Double) measurementValues.get("time_ms"))
                .addField("code ", (String) measurementValues.get("code"))
                .build();
    }

    private static Point writeDNSDataToDB(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(DNS_TYPE)
                .time(Objects.requireNonNull(convertStringToTimestamp((String) jsonObject.get("timestamp"))).getTime(), TimeUnit.MILLISECONDS)
                .addField("address", (String) measurementValues.get("address"))
                .addField("real_hostname", (String) measurementValues.get("real_hostname"))
                .addField("time_ms", (Double) measurementValues.get("time_ms"))
                .build();
    }

    private static Point writePingDataToDB(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(PING_TYPE)
                .time(Objects.requireNonNull(convertStringToTimestamp((String) jsonObject.get("timestamp"))).getTime(), TimeUnit.MILLISECONDS)
                .addField("target_ip", (String) measurementValues.get("target_ip"))
                .addField("ping_method ", (Integer) measurementValues.get("ping_method"))
                .addField("mean_rtt_ms", (Double) measurementValues.get("mean_rtt_ms"))
                .addField("max_rtt_ms", (Double) measurementValues.get("max_rtt_ms"))
                .addField("stddev_rtt_ms ", (Double) measurementValues.get("stddev_rtt_ms"))
                .build();
    }

    private static Point writeTCPDataToDB(JSONObject jsonObject) {
        JSONObject measurementValues = (JSONObject) jsonObject.get("values");
        return Point.measurement(TCP_TYPE)
                .time(Objects.requireNonNull(convertStringToTimestamp((String) jsonObject.get("timestamp"))).getTime(), TimeUnit.MILLISECONDS)
                .addField("tcp_speed_results", String.valueOf(measurementValues.get("tcp_speed_results")))
                .addField("data_limit_exceeded", (Integer) measurementValues.get("data_limit_exceeded"))
                .addField("duration", (Double) measurementValues.get("duration"))
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
        /* Config file will look like this
         databaseAddress: [value]
         databaseName: [value]
         userName: [value]
         password: [value]
         */
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

    }
}