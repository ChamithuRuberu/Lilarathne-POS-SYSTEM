package com.devstack.pos.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.devstack.pos.view.tm.TokenRequest;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;


@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.refresh.validity}")
    private int refreshValidity;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.validity}")
    private int jwtValidity;

    public String createJwtToken(TokenRequest tokenRequest) {
        log.info("Creating JWT token for user: {} with role: {}", tokenRequest.getUsername(), tokenRequest.getRole());
        String token = JWT.create()
                .withSubject(tokenRequest.getUsername())
                .withClaim("role", tokenRequest.getRole())
                .withIssuedAt(Date.from(tokenRequest.getNow().atZone(ZoneId.systemDefault()).toInstant()))
                .withIssuer("GreenCodeSolution")
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtValidity * 1000L))
                .sign(Algorithm.HMAC512(jwtSecret.getBytes()));
        log.info("JWT token created successfully");
        return token;
    }


    public String createRefreshToken(TokenRequest tokenRequest) {
        return JWT.create()
                .withSubject(tokenRequest.getUsername())
                .withClaim("role", tokenRequest.getRole())
                .withIssuedAt(Date.from(tokenRequest.getNow().atZone(ZoneId.systemDefault()).toInstant()))
                .withIssuer("GreenSphare")
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshValidity * 1000L))
                .sign(Algorithm.HMAC512(jwtSecret.getBytes()));

    }


    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            String role = decodedJWT.getClaim("role").asString();
            log.info("Extracted role from JWT: {}", role);
            return role;
        } catch (Exception e) {
            log.error("Failed to extract role from token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the expiration date from a JWT token
     * @param token JWT token
     * @return Expiration date, or null if token is invalid
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret.getBytes());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("GreenCodeSolution")
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getExpiresAt();
        } catch (Exception e) {
            log.error("Failed to get expiration date from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a JWT token is expired
     * @param token JWT token
     * @return true if token is expired or invalid, false if still valid
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            if (expirationDate == null) {
                return true; // Invalid token is considered expired
            }
            return expirationDate.before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true; // On error, consider expired
        }
    }
}
