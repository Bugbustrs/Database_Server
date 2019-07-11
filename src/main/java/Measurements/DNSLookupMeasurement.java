package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Measurement(name="dns_lookup", timeUnit = TimeUnit.MILLISECONDS)
public class DNSLookupMeasurement {
    @Column(name="time")
    private Instant time;

    @Column(name = "address")
    private String hostAddress;

    @Column(name="real_hostname")
    private String hostName;

    @Column(name ="time_ms")
    private Double timeTaken;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Double getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Double timeTaken) {
        this.timeTaken = timeTaken;
    }
}
