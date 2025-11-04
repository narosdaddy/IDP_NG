package com.ngstars.idp.service;


import com.ngstars.idp.entity.RefreshToken;
import com.ngstars.idp.entity.User;
import com.ngstars.idp.entity.VerificationToken;
import com.ngstars.idp.repository.RefreshTokenRepository;
import com.ngstars.idp.repository.VerificationTokenRepository;
import com.ngstars.idp.util.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation de TokenService.
 * - Génère refresh token opaque (UUID)
 * - Persiste refresh tokens et gère révocation
 * - Gère verification tokens (activation compte)
 */
@Service
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    @SuppressWarnings("unused")
    private final JwtTokenProvider jwtTokenProvider;
    @SuppressWarnings("unused")
    private final com.ngstars.idp.config.JwtProperties jwtProperties;

    public TokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                            VerificationTokenRepository verificationTokenRepository,
                            JwtTokenProvider jwtTokenProvider,
                            com.ngstars.idp.config.JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, Instant expiryDate, String deviceInfo) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString()); // valeur opaque sécurisée
        rt.setUser(user);
        rt.setExpiryDate(expiryDate);
        rt.setRevoked(false);
        rt.setDeviceInfo(deviceInfo);
        return refreshTokenRepository.save(rt);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(RefreshToken rt) {
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
    }

    @Override
    @Transactional
    public void revokeAllUserRefreshTokens(User user) {
        // Simple: supprimer ou marquer revocation
        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    // Verification token
    @Override
    @Transactional
    public String createVerificationToken(User user, Instant expiryDate) {
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(expiryDate);
        token.setUsed(false);
        VerificationToken saved = verificationTokenRepository.save(token);
        return saved.getToken();
    }

    @Override
    public boolean validateVerificationToken(String token) {
        Optional<VerificationToken> ot = verificationTokenRepository.findByToken(token);
        if (ot.isEmpty()) return false;
        VerificationToken vt = ot.get();
        if (vt.isExpired() || vt.isUsed()) return false;
        return true;
    }

    @Override
    public Optional<Long> getUserIdFromVerificationToken(String token) {
        return verificationTokenRepository.findByToken(token).map(v -> v.getUser().getId());
    }
}

