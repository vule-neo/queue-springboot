package com.queue.backend.users;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Pravi i cita JWT.
 *
 * Token je samo tekst u tri dijela odvojena tackom: header.payload.potpis.
 * Prva dva su obican base64 - SVAKO ih moze procitati, nista nije sakriveno.
 * Sigurnost je u trecem dijelu: potpis pravljen tajnim kljucem. Ako neko
 * promijeni payload, potpis se vise ne slaze i token je bezvrijedan.
 *
 * Zato u token NIKAD ne ide nista tajno - samo ono sto smije biti javno.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long validityMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.validityMs = expirationMinutes * 60_000;
    }

    public String generate(User user) {
        Date sada = new Date();
        return Jwts.builder()
                // subject = ko je korisnik. Email, ne id - citljivije pri debugovanju.
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(sada)
                // Rok trajanja je jedini nacin da token prestane vrijediti -
                // server ga ne pamti, pa ga ne moze ni ponistiti.
                .expiration(new Date(sada.getTime() + validityMs))
                .signWith(key)
                .compact();
    }

    /**
     * Provjerava potpis i rok, pa vraca email iz tokena.
     * Baca JwtException ako je token krivotvoren, istekao ili neispravan.
     */
    public String extractEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)   // ovdje puca ako potpis ne valja
                .getPayload();
        return claims.getSubject();
    }
}
