package com.ngstars.idp.service;


import com.ngstars.idp.entity.RefreshToken;
import com.ngstars.idp.entity.User;

import java.time.Instant;
import java.util.Optional;

/**
 * Service pour gérer refresh tokens et verification tokens.
 */
public interface TokenService {
    RefreshToken createRefreshToken(User user, Instant expiryDate, String deviceInfo);
    Optional<RefreshToken> findByToken(String token);
    void revokeRefreshToken(RefreshToken rt);
    void revokeAllUserRefreshTokens(User user);

    // Verification token
    String createVerificationToken(User user, Instant expiryDate);
    boolean validateVerificationToken(String token);
    Optional<Long> getUserIdFromVerificationToken(String token);
}

