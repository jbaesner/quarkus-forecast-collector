package org.solectrus.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ForecastConfiguration {

    @Inject
    Logger logger;

    public static final String FORECAST_SIMULATE_KEY = "forecast.%ssimulate";
    
    private static final String DEFAULT_CONFIGURATION = "default";
    
    private static final String LATITUDE_KEY = "forecast.%slatitude";
    private static final String LONGITUDE_KEY = "forecast.%slongitude";
    private static final String DECLINATION_KEY = "forecast.%sdeclination"; 
    private static final String AZIMUTH_KEY = "forecast.%sazimuth";
    private static final String KWP_KEY = "forecast.%skwp";
    
    @Inject
    @ConfigProperty(name = "forecast.configurations", defaultValue = DEFAULT_CONFIGURATION)
    Set<String> forecastConfigurations;
    
    private Config config = ConfigProvider.getConfig();

    private Map<String, ForecastConfigurationData> forecastConfigurationData = new HashMap<>(); 
    
    @PostConstruct
    /* package */ void initConfiguration() {
        
        for(String configurationName : forecastConfigurations) {

            final String configurationNameKey = toKey(configurationName);

            ForecastConfigurationData data = new ForecastConfigurationData(configurationName);
            data.setLatitude(config.getValue(String.format(LATITUDE_KEY, configurationNameKey), String.class));
            data.setLongitude(config.getValue(String.format(LONGITUDE_KEY, configurationNameKey), String.class));
            data.setDeclination(config.getValue(String.format(DECLINATION_KEY, configurationNameKey), String.class));
            data.setAzimuth(config.getValue(String.format(AZIMUTH_KEY, configurationNameKey), String.class));
            data.setKwp(config.getValue(String.format(KWP_KEY, configurationNameKey), String.class));
            
            logger.infof("parsed configuration %s", data);
            
            forecastConfigurationData.put(configurationName, data);
        }
    }
    
    public Set<String> getForecastConfigurationNames() {
        return forecastConfigurations;
    }
    
    public String getUri(String configurationName) {
        
        logger.infof("Create URI for configuration [%s]", configurationName);
        
        ForecastConfigurationData configurationData = forecastConfigurationData.get(configurationName);
        
        return String.format("/estimate/%s/%s/%s/%s/%s?time=utc", //
                configurationData.getLatitude(), //
                configurationData.getLongitude(), //
                configurationData.getDeclination(), //
                configurationData.getAzimuth(), //
                configurationData.getKwp() //
                );
    }
    
    public String getKwp(String configurationName) {
        return forecastConfigurationData.get(configurationName).getKwp();
    }
    
    public String getSimulatedResponse(String configurationName) {
        return config.getValue(String.format(FORECAST_SIMULATE_KEY, toKey(configurationName)), String.class);
    }
    
    private String toKey(String configurationName) {
        return (DEFAULT_CONFIGURATION.equals(configurationName)) ? "" : String.format("%s.", configurationName); 
    }

    private class ForecastConfigurationData {
        
        private String configurationName, latitude, longitude, declination, azimuth, kwp, simulatedResponse;
        
        public ForecastConfigurationData(String configurationName) {
            this.configurationName = configurationName;
        }
        
        public String getLatitude() {
            return latitude;
        }
        
        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public String getDeclination() {
            return declination;
        }

        public void setDeclination(String declination) {
            this.declination = declination;
        }

        public String getAzimuth() {
            return azimuth;
        }

        public void setAzimuth(String azimuth) {
            this.azimuth = azimuth;
        }

        public String getKwp() {
            return kwp;
        }

        public void setKwp(String kwp) {
            this.kwp = kwp;
        }
        
        public String getSimulatedResponse() {
            return simulatedResponse;
        }

        public void setSimulatedResponse(String simulatedResponse) {
            this.simulatedResponse = simulatedResponse;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            
            sb.append("[").append(configurationName).append("] "); 
            sb.append("latitude: ").append(latitude).append(", ");
            sb.append("longitude: ").append(longitude).append(", ");
            sb.append("declination: ").append(declination).append(", ");
            sb.append("azimuth: ").append(azimuth).append(", ");
            sb.append("kwp: ").append(kwp);
            
            return sb.toString();
        }
    }
}
