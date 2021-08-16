package org.solectrus.influx;

import org.solectrus.Forecast;

public interface DataServiceInterface {

    Forecast createForecastData(String time, Integer watt, boolean writeToInfluxDb) throws DataServiceException;
}
