package com.ngstars.idp.security;


import com.ngstars.idp.entity.User;
import com.ngstars.idp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service qui charge l'entité User depuis la DB et la transforme en UserDetails.
 * Utilisé par Spring Security pour l'authentification.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge l'utilisateur par email (username = email).
     * @param username email
     * @return UserDetails (CustomUserDetails)
     * @throws UsernameNotFoundException si non trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé pour email: " + username));
        return new CustomUserDetails(u);
    }
}

