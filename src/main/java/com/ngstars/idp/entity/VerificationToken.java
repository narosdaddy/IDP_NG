package com.ngstars.idp.entity;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Token pour verification d'email (activation de compte).
 * Peut être réutilisé ou transformé en table générique "Token" avec type.
 */
@Entity
@Table(name = "verification_tokens", indexes = {
        @Index(name = "idx_verif_token_token", columnList = "token"),
        @Index(name = "idx_verif_token_user_id", columnList = "user_id")
})
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    public VerificationToken() {}

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }

    // equals/hashCode...
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VerificationToken)) return false;
        VerificationToken that = (VerificationToken) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}

