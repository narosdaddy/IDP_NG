package com.ngstars.idp.controller;


import com.ngstars.idp.dto.AuthResponse;
import com.ngstars.idp.dto.LoginRequest;
import com.ngstars.idp.dto.RefreshTokenRequest;
import com.ngstars.idp.dto.RegisterRequest;
import com.ngstars.idp.service.AuthService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller REST pour endpoints d'authentification.
 * - register -> crée user + envoie verification
 * - verify  -> active compte
 * - login   -> renvoie access + refresh
 * - refresh -> échange refresh token pour nouveau access
 * - logout  -> révoque refresh token
 *
 * Les réponses sont simples; adapte selon besoins (statuts HTTP, body détaillé).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register : enregistre l'utilisateur et envoie un mail de vérification (implémenter en service).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // Construire appUrl pour lien verification (extraction du host)
        String appUrl = httpRequest.getRequestURL().toString().replace(httpRequest.getRequestURI(), "");
        AuthResponse resp = authService.register(request, appUrl);
        // Renvoie 201 Created si tu veux (ici 200 pour simplicité)
        return ResponseEntity.ok(resp);
    }

    /**
     * Vérification du compte via token.
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token) {
        boolean ok = authService.verifyAccount(token);
        if (ok) return ResponseEntity.ok(Map.of("message", "Compte vérifié"));
        return ResponseEntity.badRequest().body(Map.of("message", "Token invalide ou expiré"));
    }

    /**
     * Login : retourne access + refresh
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        AuthResponse resp = authService.login(request, deviceInfo);
        return ResponseEntity.ok(resp);
    }

    /**
     * Refresh token : retourne nouveau access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Validated @RequestBody RefreshTokenRequest req) {
        AuthResponse resp = authService.refreshToken(req.getRefreshToken());
        return ResponseEntity.ok(resp);
    }

    /**
     * Logout : révoque refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Validated @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Déconnecté"));
    }
}

