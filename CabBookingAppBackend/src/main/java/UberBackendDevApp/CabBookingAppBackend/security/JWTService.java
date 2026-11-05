package UberBackendDevApp.CabBookingAppBackend.security;

import UberBackendDevApp.CabBookingAppBackend.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and verifies JWTs. Access and refresh tokens carry a {@code type} claim so a long-lived
 * refresh token can never be used to authenticate API calls (and vice versa).
 */
@Service
public class JWTService {

    static final String TYPE_CLAIM = "type";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${jwt.accessTokenMinutes:10}")
    private long accessTokenMinutes;

    @Value("${jwt.refreshTokenDays:30}")
    private long refreshTokenDays;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().toString())
                .claim(TYPE_CLAIM, ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenMinutes * 60_000L))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TYPE_CLAIM, REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenDays * 24L * 60 * 60 * 1000))
                .signWith(getSecretKey())
                .compact();
    }

    public Long getUserIdFromAccessToken(String token) {
        return parse(token, ACCESS);
    }

    public Long getUserIdFromRefreshToken(String token) {
        return parse(token, REFRESH);
    }

    private Long parse(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Wrong token type: expected a " + expectedType + " token");
        }
        return Long.valueOf(claims.getSubject());
    }
}
