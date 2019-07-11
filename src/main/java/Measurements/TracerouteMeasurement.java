package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name="traceroute")
public class TracerouteMeasurement {
    @Column(name="time")
    private Instant time;

    @Column(name="num_hops")
    private Integer numberOfHops;

    @Column(name="hop_N_addr_i")
    private String listOfHopsIPAddress;

    @Column(name="hop_N_rtt_ms")
    private String listOfRTTs;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Integer getNumberOfHops() {
        return numberOfHops;
    }

    public void setNumberOfHops(Integer numberOfHops) {
        this.numberOfHops = numberOfHops;
    }

    public String getListOfHopsIPAddress() {
        return listOfHopsIPAddress;
    }

    public void setListOfHopsIPAddress(String listOfHopsIPAddress) {
        this.listOfHopsIPAddress = listOfHopsIPAddress;
    }

    public String getListOfRTTs() {
        return listOfRTTs;
    }

    public void setListOfRTTs(String listOfRTTs) {
        this.listOfRTTs = listOfRTTs;
    }
}
