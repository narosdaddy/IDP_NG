package com.ngstars.idp.service;


/**
 * Abstraction d'envoi d'email pour faciliter le remplacement par une implémentation
 * réelle (SMTP) ou un mock pendant les tests.
 */
public interface MailService {
    void sendEmail(String to, String subject, String body);
}

