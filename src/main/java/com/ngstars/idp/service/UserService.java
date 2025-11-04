package com.ngstars.idp.service;


import com.ngstars.idp.entity.User;
import java.util.Optional;

/**
 * Port pour opérations utilisateur.
 */
public interface UserService {
    User createUser(String email, String rawPassword);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    void enableUser(User user);
}

