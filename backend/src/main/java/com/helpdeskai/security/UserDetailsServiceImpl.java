package com.helpdeskai.security;

import com.helpdeskai.entity.User;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of Spring Security's UserDetailsService.
 * Loads user from database for authentication.
 */
@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by username (which is actually user ID in our case).
     * Called by JwtAuthenticationFilter during authentication.
     *
     * @param username User ID as string
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // Parse username as user ID
            Long userId = Long.parseLong(username);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with ID: " + userId));

            log.debug("User loaded for authentication: ID={}, Email={}",
                      user.getId(), user.getEmail());

            // User entity already implements UserDetails
            return user;

        } catch (NumberFormatException e) {
            // If username is not a number, try to find by email as fallback
            log.debug("Username is not a number, trying to load by email: {}", username);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with email: " + username));

            return user;
        }
    }

    /**
     * Loads user by email (alternative method for convenience).
     *
     * @param email User email
     * @return User entity
     */
    @Transactional(readOnly = true)
    public User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Loads user by ID (alternative method for convenience).
     *
     * @param userId User ID
     * @return User entity
     */
    @Transactional(readOnly = true)
    public User loadUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
