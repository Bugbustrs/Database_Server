import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.influxdb.BuilderException;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
//import org.influxdb.annotation.TimeColumn;
import org.influxdb.impl.Preconditions;
@Measurement(name="cpu")
public class Cpu{
//@TimeColumn
//@Column(name = "time")
//public Instant time;
@Column(name="host")
public String host;
@Column(name="region")
public String region;
@Column(name="value") 
public double value;

public String toString(){
	return ""+host+" "+region+" "+value;
}



}
