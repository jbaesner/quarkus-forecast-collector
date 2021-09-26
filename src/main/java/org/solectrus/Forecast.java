package org.solectrus;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

@Measurement(name = "Forecast")
public class Forecast {

    Set<DateTimeFormatter> formatters = Stream.of(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")).collect(Collectors.toCollection(HashSet::new)); 
    
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
        
        Exception ex = null;
        for(DateTimeFormatter formatter : formatters) {
            try {
                this.time = ZonedDateTime.parse(time, formatter).toInstant();
                return;
            } catch (Exception e) {
                ex = e;
                continue;
            }
        }
        throw new RuntimeException("Unable to parse DateTime", ex);
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
