package com.queue.backend.common;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Hvata izuzetke iz SVIH kontrolera na jednom mjestu.
 * Bez ovoga bi svaki izuzetak izasao kao 500 sa Spring-ovom default stranicom.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(ex.getMessage()));
    }

    // Pao @Valid na @RequestBody - vrati koje polje i zasto.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ApiError("Neispravan zahtjev", details));
    }

    // Neuspjesan login. 401, i NAMJERNO ista poruka bez obzira da li je
    // email nepoznat ili lozinka pogresna - drugacije poruke bi napadacu
    // otkrile koji su emailovi registrovani.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("Pogresan email ili lozinka"));
    }

    // @PreAuthorize je odbio poziv. 403, ne 401: znamo ko si, ali nemas prava.
    // Hvatanjem ovdje odgovor dobija isti JSON oblik kao sve ostale greske,
    // i izbjegava se ERROR dispatch koji bi status pretvorio u 401.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("Nemas prava za ovu operaciju"));
    }

    // Prelaz koji state machine ne dozvoljava. 409 Conflict: zahtjev je
    // ispravan, ali se kosi sa trenutnim stanjem ticketa.
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(ex.getMessage()));
    }

    // Pukao constraint u bazi (npr. duplikat slug-a). 409, ne 500 -
    // nije greska servera nego sukob sa postojecim podacima.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("Podatak krsi ogranicenje u bazi (mozda vec postoji)"));
    }
}
