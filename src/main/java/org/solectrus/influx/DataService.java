package org.solectrus.influx;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.solectrus.Forecast;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;

import io.quarkus.runtime.Startup;

@Startup
@Singleton
public class DataService implements DataServiceInterface, AutoCloseable {

    @Inject
    Logger logger;

    private InfluxDBClient influxDBClient;

    private static final String INFLUX_BUCKET_ID = ConfigProvider.getConfig().getValue("influx.bucket.id", String.class);
    private static final String INFLUX_ORG = ConfigProvider.getConfig().getValue("influx.org", String.class);
    private static final String INFLUX_URL = ConfigProvider.getConfig().getValue("influx.url", String.class);
    private static final String INFLUX_TOKEN = ConfigProvider.getConfig().getValue("influx.token", String.class);

    public DataService() {
    }

    @PostConstruct
    /*package*/ void initializeInfluxDBClient() {
        logger.infof("Connecting to: %s, token: %s, org: %s, bucketId: %s", INFLUX_URL, INFLUX_TOKEN, INFLUX_ORG, INFLUX_BUCKET_ID);
        this.influxDBClient = InfluxDBClientFactory.create(INFLUX_URL, INFLUX_TOKEN.toCharArray(), INFLUX_ORG, INFLUX_BUCKET_ID);
    }

    @Override
    public void close() throws Exception {
        this.influxDBClient.close();
    }

    @Override
    public Forecast createForecastData(String time, Integer watt, boolean writeToInfluxDb) throws DataServiceException {

        WriteApi writeApi = null;
        try {
            
            if(writeToInfluxDb) {
                 writeApi = influxDBClient.getWriteApi();
            }
            
            Forecast forecast = new Forecast();
            forecast.setTime(time);
            forecast.setWatt(watt);

            if(writeApi != null) {
                writeApi.writeMeasurement(WritePrecision.S, forecast);
                writeApi.close();
            }
            
            return forecast;

        } catch (Exception e) {
            throw new DataServiceException("Error while writing data to Influx: " + e.getMessage(), e);
        }
    }

}
