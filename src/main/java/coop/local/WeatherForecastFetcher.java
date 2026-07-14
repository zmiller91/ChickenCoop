package coop.local;

import com.google.gson.Gson;
import coop.device.protocol.event.MetricEvent;
import coop.device.types.DeviceType;
import coop.device.types.weatherforecast.WeatherForecastSignals;
import coop.device.types.weatherforecast.WeatherForecastSource;
import coop.local.listener.EventProcessor;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fetches an external forecast for each WEATHER_FORECAST component's configured location and reports it as
 * ordinary metric events - see WeatherForecastSource for why this is modeled as a virtual device's metrics
 * rather than a new kind of rule trigger. Runs on Spring's own scheduling thread pool (coop.local.Main already
 * has @EnableScheduling; this is the first thing to actually use it), never on PiRunner's loop thread, so a
 * slow/hanging HTTP call can't block Scheduler/RuleProcessor/DownlinkDispatcher ticks.
 */
@Component
@Log4j2
public class WeatherForecastFetcher {

    private static final Duration HORIZON = Duration.ofHours(24);
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private LocalStateProvider provider;

    // initialDelay gives CoopRunner's own startup thread (init() -> provider.init(), which is what actually
    // populates provider.getConfig()) a head start - without it, this task's first tick (Spring runs
    // @Scheduled tasks immediately at startup by default) can easily race ahead of CoopRunner and find
    // config still null, silently no-op, then not retry for a full fixedDelay period.
    @Scheduled(initialDelay = 30, fixedDelay = 3600, timeUnit = TimeUnit.SECONDS)
    public void fetch() {

        CoopState coop = provider.getConfig();
        if(coop == null || coop.getComponents() == null) {
            log.warn("Skipping weather forecast fetch - no config loaded yet.");
            return;
        }

        for(ComponentState component : coop.getComponents()) {
            if(component.getDeviceType() != DeviceType.WEATHER_FORECAST) {
                continue;
            }

            try {
                fetchForComponent(coop, component);
            } catch (Exception e) {
                log.error("Failed to fetch weather forecast for component " + component.getComponentId(), e);
            }
        }
    }

    private void fetchForComponent(CoopState coop, ComponentState component) throws Exception {

        Map<String, String> config = component.getConfig();
        String latitude = config == null ? null : config.get(WeatherForecastSource.LATITUDE.getKey());
        String longitude = config == null ? null : config.get(WeatherForecastSource.LONGITUDE.getKey());

        if(latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
            log.warn("Weather forecast component " + component.getComponentId() + " has no location configured yet.");
            return;
        }

        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
                        + "&hourly=precipitation_probability,precipitation,temperature_2m,relative_humidity_2m,"
                        + "wind_speed_10m,cloud_cover,et0_fao_evapotranspiration,dew_point_2m,uv_index,"
                        + "shortwave_radiation"
                        + "&temperature_unit=fahrenheit&wind_speed_unit=mph"
                        + "&timeformat=unixtime&timezone=UTC&forecast_days=2&past_days=1",
                latitude, longitude);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            log.warn("Open-Meteo returned " + response.statusCode() + " for component " + component.getComponentId());
            return;
        }

        ForecastResponse forecast = GSON.fromJson(response.body(), ForecastQuery.class).getHourly();
        if(forecast == null || forecast.time == null) {
            log.warn("Open-Meteo response missing hourly data for component " + component.getComponentId());
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        long horizonEnd = now + HORIZON.toSeconds();
        long horizonStart = now - HORIZON.toSeconds();

        double maxProbability = 0;
        double totalPrecipitation = 0;
        double actualPrecipitation = 0;
        boolean anyFuture = false;
        boolean anyPast = false;
        int snapshotIndex = -1;

        for(int i = 0; i < forecast.time.size(); i++) {
            long t = forecast.time.get(i);

            // First hourly point at or after now - used below for the point-in-time snapshot metrics (a
            // rolling max/sum like rain doesn't make sense for temperature/humidity/etc).
            if(snapshotIndex < 0 && t >= now) {
                snapshotIndex = i;
            }

            // Trailing 24h window (needs past_days=1 on the request) - "did it actually rain," which the
            // forward-looking RAIN_AMOUNT_24H below can't answer after the fact.
            if(t >= horizonStart && t < now) {
                anyPast = true;
                if(forecast.precipitation != null && i < forecast.precipitation.size()) {
                    actualPrecipitation += forecast.precipitation.get(i);
                }
            }

            if(t < now || t > horizonEnd) {
                continue;
            }

            anyFuture = true;
            if(forecast.precipitation_probability != null && i < forecast.precipitation_probability.size()) {
                maxProbability = Math.max(maxProbability, forecast.precipitation_probability.get(i));
            }
            if(forecast.precipitation != null && i < forecast.precipitation.size()) {
                totalPrecipitation += forecast.precipitation.get(i);
            }
        }

        if(!anyFuture) {
            log.warn("Open-Meteo response had no hourly points in the next " + HORIZON.toHours()
                    + "h for component " + component.getComponentId());
            return;
        }

        dispatch(coop, component, WeatherForecastSignals.RAIN_PROBABILITY_24H.name(), maxProbability);
        dispatch(coop, component, WeatherForecastSignals.RAIN_AMOUNT_24H.name(), totalPrecipitation);

        if(anyPast) {
            dispatch(coop, component, WeatherForecastSignals.RAIN_ACTUAL_24H.name(), actualPrecipitation);
        } else {
            log.warn("Open-Meteo response had no hourly points in the past " + HORIZON.toHours()
                    + "h for component " + component.getComponentId());
        }

        if(snapshotIndex < 0) {
            log.warn("No current/upcoming hourly point found for snapshot metrics for component " + component.getComponentId());
            return;
        }

        dispatchSnapshot(coop, component, WeatherForecastSignals.TEMPERATURE, forecast.temperature_2m, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.HUMIDITY, forecast.relative_humidity_2m, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.WIND_SPEED, forecast.wind_speed_10m, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.CLOUD_COVER, forecast.cloud_cover, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.EVAPOTRANSPIRATION, forecast.et0_fao_evapotranspiration, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.DEW_POINT, forecast.dew_point_2m, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.UV_INDEX, forecast.uv_index, snapshotIndex);
        dispatchSnapshot(coop, component, WeatherForecastSignals.SOLAR_RADIATION, forecast.shortwave_radiation, snapshotIndex);
    }

    private void dispatchSnapshot(CoopState coop, ComponentState component, WeatherForecastSignals signal,
                                   List<Double> series, int index) {
        if(series == null || index >= series.size()) {
            return;
        }
        dispatch(coop, component, signal.name(), series.get(index));
    }

    private void dispatch(CoopState coop, ComponentState component, String metric, double value) {
        MetricEvent event = new MetricEvent(component.getSerialNumber(), metric, value);
        EventProcessor.receiveSyntheticEvent(coop, component, event);
    }

    @Data
    private static class ForecastQuery {
        private ForecastResponse hourly;
    }

    @Data
    private static class ForecastResponse {
        private List<Long> time;
        private List<Double> precipitation_probability;
        private List<Double> precipitation;
        private List<Double> temperature_2m;
        private List<Double> relative_humidity_2m;
        private List<Double> wind_speed_10m;
        private List<Double> cloud_cover;
        private List<Double> et0_fao_evapotranspiration;
        private List<Double> dew_point_2m;
        private List<Double> uv_index;
        private List<Double> shortwave_radiation;
    }
}
