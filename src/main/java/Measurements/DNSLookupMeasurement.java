package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Measurement(name="dns_lookup", timeUnit = TimeUnit.MILLISECONDS)
public class DNSLookupMeasurement extends Measurements {
    @Column(name = "time")
    private Instant time;

    @Column(name = "isExperiment")
    private boolean isExperiment;

    @Column(name="JobID", tag = true)
    private String taskKey;

    @Column(name = "username")
    private String userName;

    @Column(name = "address")
    private String hostAddress;

    @Column(name="real_hostname")
    private String hostName;

    @Column(name ="time_ms")
    private Double timeTaken;

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
