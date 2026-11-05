package UberBackendDevApp.CabBookingAppBackend.unit;

import UberBackendDevApp.CabBookingAppBackend.entities.User;
import UberBackendDevApp.CabBookingAppBackend.entities.enums.Role;
import UberBackendDevApp.CabBookingAppBackend.security.JWTService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JWTServiceTest {

    private JWTService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", "unit-test-secret-key-with-at-least-32-chars!!");
        ReflectionTestUtils.setField(jwtService, "accessTokenMinutes", 10L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenDays", 30L);
        user = new User();
        user.setId(42L);
        user.setEmail("rider@example.com");
        user.setRoles(Set.of(Role.RIDER));
    }

    @Test
    void accessTokenRoundTrip() {
        assertEquals(42L, jwtService.getUserIdFromAccessToken(jwtService.generateAccessToken(user)));
    }

    @Test
    void refreshTokenRoundTrip() {
        assertEquals(42L, jwtService.getUserIdFromRefreshToken(jwtService.generateRefreshToken(user)));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        assertThrows(JwtException.class, () -> jwtService.getUserIdFromAccessToken(jwtService.generateRefreshToken(user)));
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() {
        assertThrows(JwtException.class, () -> jwtService.getUserIdFromRefreshToken(jwtService.generateAccessToken(user)));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user);
        assertThrows(JwtException.class, () -> jwtService.getUserIdFromAccessToken(token.substring(0, token.length() - 3) + "abc"));
    }
}
