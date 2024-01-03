package coop.api.auth.admin;

import com.google.gson.Gson;
import coop.database.repository.MetricRepository;
import coop.database.table.Coop;
import lombok.Data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PublishWeather {

    //https://archive-api.open-meteo.com/v1/era5?latitude=41.661129&longitude=-91.530167&start_date=2023-01-01&end_date=2023-12-30&hourly=temperature_2m,relative_humidity_2m&temperature_unit=fahrenheit&timeformat=unixtime

    private static final String COMPONENT_ID = "component-1234";

    private final WeatherData data;
    private final MetricRepository metricRepository;

    public PublishWeather(MetricRepository metricRepository, String path) {
        this.metricRepository = metricRepository;
        this.data = data(path);
    }

    public void execute(Coop coop) {
        int count = 0;
        int idx = 0;
        while(idx < this.data.time.size()) {

            Long time = this.data.time.get(idx) * 1000;
            Double temp = this.data.temperature_2m.get(idx);
            Double rh = this.data.relative_humidity_2m.get(idx);

            if (temp != null && rh != null) {
                count++;
                metricRepository.save( coop, COMPONENT_ID, time, "temperature", temp);
                metricRepository.save( coop, COMPONENT_ID, time, "humidity", rh);
            }

            if (idx % 25 == 0) {
                System.out.println(idx + " of " + this.data.time.size());
            }

            idx++;
        }

        System.out.println(count);
    }

    private WeatherData data(String path) {
        try {
            String data = new String(Files.readAllBytes(Paths.get(path)));
            return new Gson().fromJson(data, WeatherQuery.class).getHourly();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Data
    private static class WeatherQuery {
        WeatherData hourly;
    }

    @Data
    private static class WeatherData {
        List<Long> time;
        List<Double> temperature_2m;
        List<Double> relative_humidity_2m;
    }

}
