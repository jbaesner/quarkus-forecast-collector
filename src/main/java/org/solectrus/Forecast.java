package org.solectrus;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

@Measurement(name = "Forecast")
public class Forecast {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    
    @Column(timestamp = true)
    private Instant time;
    
    @Column
    private int watt;
    
    public Forecast() {}

    public Instant getTime() {
        return time;
    }
    
    public void setTime(Instant time) {
        this.time = time;
    }
    
    public void setTime(String time) {
        this.time = ZonedDateTime.parse(time, formatter).toInstant();
    }
    
    public int getWatt() {
        return watt;
    }
    
    public void setWatt(int watt) {
        this.watt = watt;
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("(").append(time).append("; ").append(watt).append(")");
        return sb.toString();
    }
    
}
