package com.ngstars.idp.util;


import com.ngstars.idp.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Fournit méthodes pour générer, parser et valider les JWT.
 * Utilise une secret key symétrique (HS256/HS512) stockée via JwtProperties.
 *
 * Attention : pour production, il est préférable d'utiliser des clés asymétriques (RS256)
 * ou une gestion avancée des clés via JWKS.
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private Key key; // clé symétrique dérivée du secret

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        // Convertit le secret en Key robuste (HS512). Ne pas utiliser secret court en prod.
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes());
    }

    /**
     * Génère un JWT pour un sujet (username/email) avec claims optionnels.
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(props.getExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(claims)
                .signWith(key, SignatureAlgorithm.HS512);

        return builder.compact();
    }

    /**
     * Génère un refresh token minimal (string long aléatoire).
     * Ici on recommande la persistance côté DB plutôt qu'un JWT lourd.
     */
    public String generateRefreshTokenString() {
        // On peut utiliser JWT ou token aléatoire. Ici token aléatoire simple:
        return Keys.secretKeyFor(SignatureAlgorithm.HS512).toString() + "-" + Instant.now().toEpochMilli();
    }

    /**
     * Valide JWT et retourne sujet (username/email).
     * Lance exception JwtException si invalide.
     */
    public String getSubjectFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    /**
     * Validate token : retourne true si valide, false sinon.
     * Tu peux logger les exceptions pour debug.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // token invalide, expired, malformed, signature invalid, etc.
            return false;
        }
    }

    /**
     * Retourne expiration Date du JWT.
     */
    public Instant getExpirationFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getExpiration().toInstant();
    }
}

