package tech.lougon.profitly.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.user.infrastructure.security.UserSecurityDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(UserSecurityDetails user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("profitly")
                    .withSubject(user.getUsername())
                    .withClaim("userId", user.getUserId())
                    .withExpiresAt(expirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new RuntimeException("Error generating token", ex);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("profitly")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException ex) {
            return "";
        }
    }

    public String extractUserId(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("profitly")
                .build()
                .verify(token)
                .getClaim("userId")
                .asString();
    }

    private Instant expirationDate() {
        return LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.of("-03:00"));
    }
}
