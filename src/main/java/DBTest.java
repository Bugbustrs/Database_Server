import org.influxdb.impl.InfluxDBImpl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.influxdb.*;
import org.influxdb.dto.Pong;
import okhttp3.OkHttpClient;


public class DBTest{
	
	public static void main(String args[]){
		InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "bugbusters", "");
		Pong response = influxDB.ping();
		if (response.getVersion().equalsIgnoreCase("unknown")) {
			System.out.println("Error pinging server.");
		      return;
		}else{
			System.out.println(response);
			return;
		}	
	}
}
