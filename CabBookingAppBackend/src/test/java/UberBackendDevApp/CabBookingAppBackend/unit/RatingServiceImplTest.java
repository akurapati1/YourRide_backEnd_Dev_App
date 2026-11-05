package UberBackendDevApp.CabBookingAppBackend.unit;

import UberBackendDevApp.CabBookingAppBackend.entities.*;
import UberBackendDevApp.CabBookingAppBackend.exceptions.RuntimeConflictException;
import UberBackendDevApp.CabBookingAppBackend.repositories.*;
import UberBackendDevApp.CabBookingAppBackend.services.impl.RatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock RatingRepository ratingRepository;
    @Mock DriverRepository driverRepository;
    @Mock RiderRepository riderRepository;
    @Mock ModelMapper modelMapper;
    @InjectMocks RatingServiceImpl ratingService;

    private Driver driver;
    private Ride ride;

    @BeforeEach
    void setUp() {
        driver = Driver.builder().id(1L).rating(0.0).build();
        ride = new Ride();
        ride.setId(10L);
        ride.setDriver(driver);
    }

    private Rating rating(Integer driverRating) {
        return Rating.builder().ride(ride).driver(driver).driverRating(driverRating).build();
    }

    @Test
    void averageIgnoresRidesThatHaveNotBeenRatedYet() {
        Rating current = rating(null);
        when(ratingRepository.findByRide(ride)).thenReturn(Optional.of(current));
        // history: two previously rated rides, the current one (now rated 5) and one still unrated (null)
        when(ratingRepository.findByDriver(driver)).thenReturn(List.of(rating(3), rating(4), rating(5), rating(null)));
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));

        ratingService.rateDriver(ride, 5);

        assertEquals(4.0, driver.getRating(), 1e-9);
        assertEquals(5, current.getDriverRating());
    }

    @Test
    void ratingTwiceIsRejected() {
        when(ratingRepository.findByRide(ride)).thenReturn(Optional.of(rating(4)));
        assertThrows(RuntimeConflictException.class, () -> ratingService.rateDriver(ride, 5));
    }

    @Test
    void ratingOutsideOneToFiveIsRejected() {
        assertThrows(RuntimeConflictException.class, () -> ratingService.rateDriver(ride, 0));
        assertThrows(RuntimeConflictException.class, () -> ratingService.rateDriver(ride, 6));
        assertThrows(RuntimeConflictException.class, () -> ratingService.rateDriver(ride, null));
    }
}
