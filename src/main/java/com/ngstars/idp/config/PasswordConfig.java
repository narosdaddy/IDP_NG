package com.ngstars.idp.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration du PasswordEncoder.
 * BCrypt est recommandé pour le hachage des mots de passe.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength par défaut (10) — augmente si besoin (coût CPU plus élevé)
        return new BCryptPasswordEncoder();
    }
}

