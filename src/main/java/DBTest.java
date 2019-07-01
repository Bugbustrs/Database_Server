import org.influxdb.impl.InfluxDBImpl;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.influxdb.*;
import org.influxdb.dto.*;
import org.influxdb.impl.InfluxDBResultMapper;
import okhttp3.OkHttpClient;
import java.util.*;

public class DBTest{
	
	public static void main(String args[]){
		InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "bugbusters", "");
		Pong response = influxDB.ping();
		if (response.getVersion().equalsIgnoreCase("unknown")) {
			System.out.println("Error pinging server.");
		      return;
		}
		QueryResult res=influxDB.query(new Query("Select * from cpu","mydb"));
		InfluxDBResultMapper resultMapper=new InfluxDBResultMapper();
		List<Cpu> list=resultMapper.toPOJO(res,Cpu.class);

		for(Cpu c:list) System.out.println(c);

	}
}
