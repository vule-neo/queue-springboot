package com.queue.backend.users;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Most izmedju Spring Security-ja i nase tabele app_user.
 *
 * Spring Security ne zna za nas User entitet - zna samo za svoj UserDetails
 * interfejs. Ova klasa prevodi jedno u drugo.
 *
 * Cim ovaj bean postoji, nestaje generisana lozinka iz loga:
 * UserDetailsServiceAutoConfiguration se povlaci.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik ne postoji"));

        // Puno ime klase jer se Spring-ov User sudara sa nasim.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                // Prefiks ROLE_ je obavezan: hasRole("ADMIN") ga sam dodaje,
                // pa bez njega provjera uloge nikad ne prolazi.
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}
