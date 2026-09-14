package com.queue.backend.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.queue.backend.users.AppUserDetailsService;
import com.queue.backend.users.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Trci na svakom zahtjevu, PRIJE nego zahtjev dodje do kontrolera.
 *
 * Posao: procitaj Authorization header, provjeri token, i ako valja
 * upisi korisnika u SecurityContext. Odatle ga kasnije citaju pravila
 * pristupa i @PreAuthorize.
 *
 * OncePerRequestFilter garantuje da se izvrsi tacno jednom po zahtjevu
 * (obican filter bi se mogao okinuti vise puta kod internih forward-ova).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                String email = jwtService.extractEmail(token);   // puca ako token ne valja

                // Korisnika ucitavamo iz baze, a ne iz tokena. Sporije za jedan
                // upit, ali tacno: ako je uloga promijenjena ili nalog obrisan,
                // stari token vise ne daje stara prava.
                UserDetails korisnik = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        korisnik, null, korisnik.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | UsernameNotFoundException e) {
                // Los token: ne rusimo zahtjev ovdje, samo ostavljamo kontekst
                // prazan. Pravila pristupa dalje u lancu ce vratiti 401.
                SecurityContextHolder.clearContext();
            }
        }

        // Bez ovoga se zahtjev zaustavlja i nikad ne stigne do kontrolera.
        chain.doFilter(request, response);
    }
}
