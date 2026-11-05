package UberBackendDevApp.CabBookingAppBackend.unit;

import UberBackendDevApp.CabBookingAppBackend.entities.*;
import UberBackendDevApp.CabBookingAppBackend.entities.enums.PaymentStatus;
import UberBackendDevApp.CabBookingAppBackend.entities.enums.TransactionMethod;
import UberBackendDevApp.CabBookingAppBackend.repositories.PaymentRepository;
import UberBackendDevApp.CabBookingAppBackend.services.DistanceService;
import UberBackendDevApp.CabBookingAppBackend.services.WalletService;
import UberBackendDevApp.CabBookingAppBackend.strategies.RideStrategyManager;
import UberBackendDevApp.CabBookingAppBackend.strategies.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrategiesTest {

    @Mock DistanceService distanceService;
    @Mock WalletService walletService;
    @Mock PaymentRepository paymentRepository;

    @Test
    void defaultFareIsDistanceTimesRate() {
        when(distanceService.calculateDistance(any(), any())).thenReturn(7.5);
        assertEquals(75.0, new RiderFareDefaultFareCalculationStrategy(distanceService).calculateFare(new RideRequest()), 1e-9);
    }

    @Test
    void surgeFareIsDoubleTheDefault() {
        when(distanceService.calculateDistance(any(), any())).thenReturn(7.5);
        assertEquals(150.0, new RideFareSurgePricingFareCalculationStrategy(distanceService).calculateFare(new RideRequest()), 1e-9);
    }

    private Payment paymentOf(double amount) {
        User riderUser = new User();
        User driverUser = new User();
        Rider rider = Rider.builder().user(riderUser).build();
        Driver driver = Driver.builder().user(driverUser).build();
        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setDriver(driver);
        return Payment.builder().ride(ride).amount(amount).paymentStatus(PaymentStatus.PENDING).build();
    }

    @Test
    void walletPaymentMovesFareFromRiderAndPaysDriverMinusCommission() {
        Payment payment = paymentOf(100.0);
        new WalletPaymentStrategy(walletService, paymentRepository).processPayment(payment);

        verify(walletService).deductMoneyFromWallet(eq(payment.getRide().getRider().getUser()), eq(100.0), isNull(), any(), eq(TransactionMethod.RIDE));
        verify(walletService).addMoneyToWallet(eq(payment.getRide().getDriver().getUser()), eq(70.0), isNull(), any(), eq(TransactionMethod.RIDE));
        assertEquals(PaymentStatus.CONFIRMED, payment.getPaymentStatus());
    }

    @Test
    void cashPaymentDeductsOnlyCommissionFromDriver() {
        Payment payment = paymentOf(100.0);
        new CashPaymentStrategy(walletService, paymentRepository).processPayment(payment);

        verify(walletService).deductMoneyFromWallet(eq(payment.getRide().getDriver().getUser()), eq(30.0), isNull(), any(), eq(TransactionMethod.RIDE));
        verifyNoMoreInteractions(walletService);
        assertEquals(PaymentStatus.CONFIRMED, payment.getPaymentStatus());
    }

    @Test
    void highlyRatedRidersGetTopRatedDriversOthersGetNearest() {
        var topRated = mock(DriverMatchingHighestRatedDriverStrategy.class);
        var nearest = mock(DriverMatchingNearestDriverStrategy.class);
        var manager = new RideStrategyManager(topRated, nearest,
                new RideFareSurgePricingFareCalculationStrategy(distanceService),
                new RiderFareDefaultFareCalculationStrategy(distanceService));
        assertSame(topRated, manager.driverMatchingStrategy(4.8));
        assertSame(nearest, manager.driverMatchingStrategy(4.79));
    }
}
