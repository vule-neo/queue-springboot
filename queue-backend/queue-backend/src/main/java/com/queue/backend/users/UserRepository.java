package com.queue.backend.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Optional jer email mozda ne postoji - repozitorij ne odlucuje
    // je li to greska, to je posao servisa.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
