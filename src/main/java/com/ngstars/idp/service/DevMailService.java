package com.ngstars.idp.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implémentation de développement du MailService.
 * Affiche dans les logs le contenu au lieu d'envoyer réellement un email.
 * Remplacer par une implémentation SMTP (Spring Mail) en production.
 */
@Service
public class DevMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(DevMailService.class);

    @Override
    public void sendEmail(String to, String subject, String body) {
        // Logging utile en dev pour récupérer le lien de vérification
        log.info("=== ENVOI EMAIL (DEV) ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", body);
        log.info("=========================");
    }
}
