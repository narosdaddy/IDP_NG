package com.ngstars.idp.service;


import com.ngstars.idp.dto.AuthResponse;
import com.ngstars.idp.dto.LoginRequest;
import com.ngstars.idp.dto.RegisterRequest;

/**
 * Service d'authentification qui orchestre UserService + TokenService + JwtTokenProvider.
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request, String appUrl /* pour lien verification */);
    AuthResponse login(LoginRequest request, String deviceInfo);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    boolean verifyAccount(String token);
}

