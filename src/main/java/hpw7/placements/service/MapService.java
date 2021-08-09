package hpw7.placements.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import hpw7.placements.domain.DurationToProvider;
import hpw7.placements.domain.Provider;
import hpw7.placements.repository.DurationToProviderRepository;
import hpw7.placements.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * The <code>MapService</code> provides methods to geocode a given address (for a provider), and to get the driving time
 * duration between two different providers / points. These methods interact with the Google Maps API.
 */
@Service
public class MapService {

    private final DurationToProviderRepository dTPRepo;

    private final ProviderRepository prRepo;

    @Value("${gmaps.api.client-key}")
    public String gmapsAPIClientKey;

    @Value("${gmaps.api.server-key}")
    private String gmapsAPIServerKey;

    private final GeoApiContext context = new GeoApiContext.Builder()
            .apiKey(gmapsAPIServerKey)
            .build();

    public MapService(DurationToProviderRepository dTPRepo, ProviderRepository prRepo) {
        this.dTPRepo = dTPRepo;
        this.prRepo = prRepo;
    }

    /**
     * Geocodes the given address into latitude and longitude. For best results, the address should ideally be as
     * detailed as possible - but since it is using the Google Maps API, it should always return the best result
     * possible.
     *
     * @param address The address to geocode
     * @return A latitude and longitude pair, as a LatLng object, representing the location of the address. If the
     * address couldn't be geocoded, will return a 0,0 pair.
     * @throws InterruptedException The call to the API was interrupted unexpectedly, before a response was returned.
     * @throws ApiException         The call to the API failed (an error response was returned).
     * @throws IOException          Some I/O exception has occurred while calling the API.
     */
    public LatLng geocodeAddress(String address) throws InterruptedException, ApiException, IOException {
        // GeoApiContext context = GeoApiContextSingleton.INSTANCE.getInstance().getContext();
        GeocodingResult[] result = GeocodingApi.geocode(context, address).await();
        if (result.length <= 0) {
            // Address couldn't be geocoded (probably, the address doesn't exist)
            return new LatLng(0.0, 0.0);
        } else {
            return GeocodingApi.geocode(context, address).await()[0].geometry.location;
        }
    }

    /**
     * Given two points pointA and pointB, works out the time it would take to drive from pointA to pointB.
     *
     * @param pointA The point to start from
     * @param pointB The point to reach
     * @return A driving time {@link Duration} between the two points
     * @throws InterruptedException The call to the API was interrupted unexpectedly, before a response was returned.
     * @throws ApiException         The call to the API failed (an error response was returned).
     * @throws IOException          Some I/O exception has occurred while calling the API.
     */
    public Duration drivingTimeBetween(LatLng pointA, LatLng pointB) throws InterruptedException, ApiException, IOException {
        // GeoApiContext context = GeoApiContextSingleton.INSTANCE.getInstance().getContext();
        // Call API to calculate "distance matrix" and wait for result
        DistanceMatrix result = DistanceMatrixApi.getDistanceMatrix(context, new String[]{pointA.toString()}, new String[]{pointB.toString()})
                .mode(TravelMode.DRIVING).await();

        return Duration.ofSeconds(result.rows[0].elements[0].duration.inSeconds);
    }

    /**
     * Given two providers p1 and p2, works out the time it would take to drive from p1 to p2.
     *
     * @param p1 The provider to start from. Must already be geocoded (so, has a latitude and longitude.)
     * @param p2 The provider to reach. Must already be geocoded.
     * @return A driving time {@link Duration} between the two providers
     * @throws InterruptedException The call to the API was interrupted unexpectedly, before a response was returned.
     * @throws ApiException         The call to the API failed (an error response was returned).
     * @throws IOException          Some I/O exception has occurred while calling the API.
     */
    public Duration drivingTimeBetween(Provider p1, Provider p2) throws InterruptedException, ApiException, IOException {
        for (DurationToProvider timeToPr2 : p1.getTimesToPr2s()) {
            if (timeToPr2.getPr2() == p2) {
                return timeToPr2.getDuration(); // The result has previously been calculated - no need to call the API.
            }
        }
        // Calling the API to get the result ...
        Duration duration = drivingTimeBetween(new LatLng(p1.getLatitude(), p1.getLongitude()),
                new LatLng(p2.getLatitude(), p2.getLongitude()));

        // ... and storing the result, to reduce unnecessary calls to the API in the future.
        DurationToProvider dTP = new DurationToProvider(p2, duration);
        p1.getTimesToPr2s().add(dTP);
        dTPRepo.save(dTP);
        prRepo.save(p1);

        DurationToProvider dTPAlt = new DurationToProvider(p1, duration);
        p2.getTimesToPr2s().add(dTPAlt);
        dTPRepo.save(dTPAlt);
        prRepo.save(p2);

        return duration;
    }

}
