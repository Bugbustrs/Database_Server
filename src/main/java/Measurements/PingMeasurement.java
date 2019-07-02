package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;


@Measurement(name = "ping")
public class PingMeasurement {
    @Column(name = "time")
    private Instant time;

    @Column(name ="target_ip ")
    private String  targetIpAddress;

    @Column(name ="ping_method ")
    private String pingMethod;

    @Column(name = "mean_rtt_ms")
    private Double meanRttMS;

    @Column(name = "max_rtt_ms")
    private Double maxRttMs;

    @Column(name = "stddev_rtt_ms")
    private Double stddevRttMs;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getTargetIpAddress() {
        return targetIpAddress;
    }

    public void setTargetIpAddress(String targetIpAddress) {
        this.targetIpAddress = targetIpAddress;
    }

    public String getPingMethod() {
        return pingMethod;
    }

    public void setPingMethod(String pingMethod) {
        this.pingMethod = pingMethod;
    }

    public Double getMeanRttMS() {
        return meanRttMS;
    }

    public void setMeanRttMS(Double meanRttMS) {
        this.meanRttMS = meanRttMS;
    }

    public Double getMaxRttMs() {
        return maxRttMs;
    }

    public void setMaxRttMs(Double maxRttMs) {
        this.maxRttMs = maxRttMs;
    }

    public Double getStddevRttMs() {
        return stddevRttMs;
    }

    public void setStddevRttMs(Double stddevRttMs) {
        this.stddevRttMs = stddevRttMs;
    }
}
