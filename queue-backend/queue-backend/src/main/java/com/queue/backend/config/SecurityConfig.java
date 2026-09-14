package com.queue.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
// Bez ovoga se @PreAuthorize TIHO ignorise - nema greske, pravila
// jednostavno ne vaze. Jedna od opasnijih nijemih gresaka u Springu.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF stiti od zloupotrebe cookie-ja koje browser salje sam.
                // Token ide u headeru, njega browser ne kaci sam - napad ne postoji.
                .csrf(csrf -> csrf.disable())

                // Server ne pamti nista izmedju zahtjeva; svaki nosi svoj token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Registracija i login moraju biti otvoreni - inace
                        // niko ne bi mogao doci do tokena.
                        .requestMatchers("/api/auth/**").permitAll()
                        // /error MORA biti otvoren. sendError() okida interni
                        // ERROR dispatch koji opet prolazi kroz ovaj lanac;
                        // tada je SecurityContext vec prazan, pa bi 403 bio
                        // pregazen sa 401.
                        .requestMatchers("/error").permitAll()
                        // WebSocket handshake je obican HTTP zahtjev pa pada
                        // pod ova pravila. Otvoren je jer je sadrzaj javan -
                        // brojevi na zidnom ekranu ionako vide svi u cekaonici.
                        // Da kroz topic idu privatni podaci, ovdje bi trebala
                        // autentikacija u STOMP CONNECT frame-u.
                        .requestMatchers("/ws/**").permitAll()
                        // Javni ekran u cekaonici nema korisnika ni token, a
                        // treba mu pocetno stanje reda (WebSocket salje samo
                        // promjene od trenutka prikljucenja). Sadrzaj je isti
                        // onaj koji visi na zidu, pa se nista ne otkriva.
                        // Samo GET - izdavanje i mijenjanje i dalje traze token.
                        .requestMatchers(HttpMethod.GET, "/api/queues/*/tickets").permitAll()
                        // Sve ostalo trazi validan token.
                        .anyRequest().authenticated())

                // Bez ovoga bi neautentikovan zahtjev vratio 403; 401 je tacnije:
                // "nisi se predstavio", a ne "predstavio si se i nemas prava".
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED, "Potreban token")))

                // Nas filter ide PRIJE standardnog login filtera, da SecurityContext
                // bude popunjen dok pravila pristupa budu provjeravana.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt je namjerno spor i sam dodaje salt.
        return new BCryptPasswordEncoder();
    }
}
