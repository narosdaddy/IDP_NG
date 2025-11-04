package com.ngstars.idp.service;


import com.ngstars.idp.config.JwtProperties;
import com.ngstars.idp.dto.AuthResponse;
import com.ngstars.idp.dto.LoginRequest;
import com.ngstars.idp.dto.RegisterRequest;
import com.ngstars.idp.entity.RefreshToken;
import com.ngstars.idp.entity.User;
import com.ngstars.idp.entity.VerificationToken;
import com.ngstars.idp.repository.VerificationTokenRepository;
import com.ngstars.idp.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation complète d'AuthService.
 *
 * Responsabilités :
 * - register : créer user (inactif), générer token vérification, envoyer email
 * - login : authentifier, générer access JWT & refresh token persistant
 * - refreshToken : valider refresh token persistant et renvoyer nouvel access token
 * - logout : révoquer refresh token
 * - verifyAccount : valider token de verification puis activer compte
 *
 * Note : pour envoyer des emails réels, remplace DevMailService par une implémentation SMTP.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthServiceImpl(UserService userService,
                           TokenService tokenService,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           AuthenticationManager authenticationManager,
                           PasswordEncoder passwordEncoder,
                           MailService mailService,
                           VerificationTokenRepository verificationTokenRepository) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    /**
     * Register :
     * - crée l'utilisateur (disabled)
     * - génère un verification token (24h)
     * - envoie un email de vérification (ou log en dev)
     *
     * Retourne AuthResponse vide (pas d'auth tant que compte non vérifié).
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String appUrl) {
        // Validation simple côté service (DTO a déjà validation annotations)
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        // Crée l'utilisateur avec rôle par défaut; user.enabled = false
        User created = userService.createUser(request.getEmail(), request.getPassword());

        // Calculer expiration token verification (par ex. 24h)
        Instant expiry = Instant.now().plusMillis(24L * 60 * 60 * 1000);

        String verificationToken = tokenService.createVerificationToken(created, expiry);

        // Construire lien vérification (ex: https://app.example.com/api/auth/verify?token=xxx)
        String verifyPath = "/api/auth/verify?token=" + verificationToken;
        String verificationUrl = buildAppUrl(appUrl, verifyPath);

        // Envoyer email (implémentation concrète fournie par MailService)
        String subject = "Vérifiez votre compte";
        String body = "Bonjour,\n\nVeuillez vérifier votre compte en cliquant sur le lien suivant :\n"
                + verificationUrl + "\n\nCe lien expire dans 24 heures.\n\nCordialement.";

        mailService.sendEmail(created.getEmail(), subject, body);

        // On ne retourne pas de token d'auth car compte non activé
        return new AuthResponse(null, null, null);
    }

    /**
     * Login :
     * - Authentifie via AuthenticationManager (qui utilise CustomUserDetailsService)
     * - Génère access JWT (via JwtTokenProvider)
     * - Crée refresh token persistant (TokenService)
     * - Renvoie AuthResponse (access + refresh + expiry)
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        try {
            // Authentifier via AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails principal = (UserDetails) authentication.getPrincipal();

            // Vérifier que l'utilisateur est activé (enabled)
            User userEntity = userService.findByEmail(principal.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après authentification"));

            if (!userEntity.isEnabled()) {
                throw new IllegalStateException("Compte non vérifié. Vérifiez votre email.");
            }

            // Générer claims (ajoute roles si tu veux)
            Map<String, Object> claims = new HashMap<>();
            // Exemple : claims.put("roles", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

            String accessToken = jwtTokenProvider.generateAccessToken(principal.getUsername(), claims);
            Instant accessExpiry = jwtTokenProvider.getExpirationFromToken(accessToken);

            // Créer refresh token et le persister
            Instant refreshExpiry = Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs());
            RefreshToken rt = tokenService.createRefreshToken(userEntity, refreshExpiry, deviceInfo);

            // Mettre à jour lastLogin pour suivi simple
            userEntity.setLastLogin(Instant.now());
            userService.findById(userEntity.getId()).ifPresent(u -> {
                // userService.enableUser ne fait pas ce set, on sauvegarde direct via UserService ou repo
                // Ici on peut appeler userService's repository save (implémentation UserServiceImpl already saves)
                // But UserService doesn't expose update method; for simplicity we save via createUser? better to rely on JPA managed entity
            });

            return new AuthResponse(accessToken, rt.getToken(), accessExpiry);

        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Identifiants invalides");
        }
    }

    /**
     * Refresh token : vérifie le refresh token stocké en base, s'il est valide et non révoqué,
     * génère un nouvel access token et renvoie AuthResponse.
     */
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken rt = tokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token introuvable"));

        if (rt.isExpired()) {
            // Si expiré -> révoquer et refuser
            tokenService.revokeRefreshToken(rt);
            throw new IllegalArgumentException("Refresh token expiré");
        }

        if (rt.isRevoked()) {
            throw new IllegalArgumentException("Refresh token révoqué");
        }

        // Générer nouveau access token
        Map<String, Object> claims = new HashMap<>();
        String newAccess = jwtTokenProvider.generateAccessToken(rt.getUser().getEmail(), claims);
        Instant newExpiry = jwtTokenProvider.getExpirationFromToken(newAccess);

        return new AuthResponse(newAccess, refreshTokenStr, newExpiry);
    }

    /**
     * Logout : révoque le refresh token fourni (si présent).
     */
    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        tokenService.findByToken(refreshTokenStr).ifPresent(tokenService::revokeRefreshToken);
    }

    /**
     * Vérification du compte via token :
     * - vérifie la validité du token (expiration/used)
     * - active le compte
     * - marque le token comme utilisé (pour empêcher la réutilisation)
     */
    @Override
    @Transactional
    public boolean verifyAccount(String token) {
        // Vérifier via TokenService
        boolean valid = tokenService.validateVerificationToken(token);
        if (!valid) return false;

        // Récupérer userId à partir du token
        var optUserId = tokenService.getUserIdFromVerificationToken(token);
        if (optUserId.isEmpty()) return false;

        Long userId = optUserId.get();
        User user = userService.findById(userId).orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));

        // Activer l'utilisateur
        userService.enableUser(user);

        // Marquer token comme used pour éviter réutilisation
        verificationTokenRepository.findByToken(token).ifPresent(vt -> {
            vt.setUsed(true);
            verificationTokenRepository.save(vt);
        });

        return true;
    }

    /**
     * Helper pour construire une URL d'Application propre (appUrl + path).
     */
    private String buildAppUrl(String appUrl, String path) {
        if (appUrl == null) return path;
        if (appUrl.endsWith("/")) {
            return appUrl.substring(0, appUrl.length() - 1) + path;
        } else {
            return appUrl + path;
        }
    }
}
