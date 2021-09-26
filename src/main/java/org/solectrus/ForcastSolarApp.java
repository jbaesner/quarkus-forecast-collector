package org.solectrus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.solectrus.config.ForecastConfiguration;
import org.solectrus.influx.DataServiceException;
import org.solectrus.influx.DataServiceInterface;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.asyncutil.util.StageSupport;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ForcastSolarApp {

    @Inject
    Logger logger;

    @Inject 
    ForecastConfiguration forecastConfiguration;
    
    @Inject
    DataServiceInterface dataSvc;

    @Inject
    ObjectMapper mapper;

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Client client;
    private String baseUrl;

    @Inject
    @ConfigProperty(name = "simulate.forecast", defaultValue = "false")
    boolean configSimulateForecast;
    
    @Inject
    @ConfigProperty(name = "write.to.influxdb", defaultValue = "true")
    boolean configWriteToInfluxDb;
    
    @Inject
    @ConfigProperty(name = "forecast.interval")
    String forecastInterval;
    
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");
    
    @Inject
    public void PostResourceClient() {
        baseUrl = "https://api.forecast.solar";
        client = ClientBuilder.newBuilder().executorService(executorService).build();
    }

    @Scheduled(every = "{forecast.interval}")
    public void execute() {
        
        String executionTime = LocalDateTime.now().format(LOCAL_DATE_TIME_FORMAT); 
        
        String method = (configSimulateForecast) ? "simulating" : "querying";

        logger.infof("[%s] %s forecast data, interval: %s", executionTime, method, forecastInterval);

        Map<String, Integer> totalForecastData = new HashMap<>();
        
        for(String configurationName : forecastConfiguration.getForecastConfigurationNames()) {
            Map<String, Integer> forecastData = getForecastDataFor(executionTime, configurationName);
            
            consolidateForecastData(forecastData, totalForecastData);
        }

        logger.trace(totalForecastData.toString());

        for (Map.Entry<String, Integer> currentWattEntry : totalForecastData.entrySet()) {
            try {
                Forecast forecast = dataSvc.createForecastData(currentWattEntry.getKey(), currentWattEntry.getValue(), configWriteToInfluxDb);
                logger.debugf("Forecast: %s", forecast);
            } catch (DataServiceException e) {
                logger.warnf(e, "Unable to write ForecastData '%s' to InfluxDB", currentWattEntry.toString());
            }
        }
    }
    
    private Map<String, Integer> getForecastDataFor(String executionTime, String configurationName) {
        
        Map<String, Integer> forecastDataMap = new HashMap<>();

        CompletionStage<String> responseFuture;
        String response;
        
        if (configSimulateForecast) {
            
            String simulatedResponse = forecastConfiguration.getSimulatedResponse(configurationName);
            
            if(simulatedResponse == null) {
                throw new IllegalStateException("No forecastResponse data found in environment for '"+String.format(forecastConfiguration.FORECAST_SIMULATE_KEY, configurationName)+"'");
            }
            
            responseFuture = StageSupport.completedStage(simulatedResponse);
        } else {
            
            String uri = String.format("%s/%s", baseUrl, forecastConfiguration.getUri(configurationName));
            
            logger.infof("Forecase URI: %s", uri);
            
            responseFuture = client.target(uri).request().rx().get(String.class);
        }

        try {
            response = responseFuture.toCompletableFuture().get();
        } catch (Exception e) {
            logger.warnf(e, "Unable to get ForecastData for configuration '%s'", configurationName);
            return forecastDataMap;
        }
        
        // log the raw JSON response
        logger.infof("// %skWp / [%s] [%s] %s", forecastConfiguration.getKwp(configurationName), executionTime, configurationName, response);
        
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(response);
        } catch (Exception e) {
            logger.warnf(e, "Unable to parse response '%s'", response);
            return forecastDataMap;
        }

        logger.warnf("%s", jsonNode.toPrettyString());
        
        Iterator<Map.Entry<String, JsonNode>> entryIterator =  jsonNode.get("result").get("watts").fields();

        while (entryIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = entryIterator.next();

            final String currentTime = entry.getKey();
            final Integer currentWatt = Integer.valueOf(entry.getValue().asText());

            forecastDataMap.put(currentTime, currentWatt);
        }
        
        return forecastDataMap;
    }


    private void consolidateForecastData(Map<String, Integer> currentForecastData,
            Map<String, Integer> consolidatedForecastData ) {

        for(Map.Entry<String, Integer> entry : currentForecastData.entrySet() ) {

            final String currentTime = entry.getKey();
            final Integer currentWatt = entry.getValue();

            Integer wattSum = consolidatedForecastData.get(currentTime);

            if (wattSum == null) {
                logger.debugf("time: %s, (current) watt: %d", currentTime, currentWatt);
                consolidatedForecastData.put(currentTime, currentWatt);
            } else {
                wattSum = wattSum.intValue() + currentWatt.intValue();
                logger.debugf("time: %s, (current) watt: %d, (new) watt: %d", currentTime, currentWatt, wattSum);
                consolidatedForecastData.put(currentTime, wattSum);
            }
        }
    }
}
