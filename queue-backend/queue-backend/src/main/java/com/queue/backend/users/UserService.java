package com.queue.backend.users;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;   // bean iz SecurityConfig
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(UserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        // Jedino mjesto gdje lozinka postoji u cistom obliku - odmah se hashuje.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);

        // Duplikat emaila ne provjeravamo rucno: uq_app_user_email to hvata,
        // a provjera-pa-upis ionako nije atomicna.
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(UserRequest request) {
        User user = userRepository.findByEmail(request.email())
                // ISTA greska kao za pogresnu lozinku - inace bi napadac kroz
                // /auth/login mogao izlistati koji emailovi postoje.
                .orElseThrow(() -> new BadCredentialsException("Pogresan email ili lozinka"));

        // matches, a ne encode pa poredjenje stringova: BCrypt svaki put
        // generise drugi salt, pa dva hasha iste lozinke nisu isti string.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Pogresan email ili lozinka");
        }

        // Lozinka je provjerena - izdaj token. Od sad se klijent predstavlja
        // njime i vise ne salje lozinku.
        return AuthResponse.of(jwtService.generate(user), user);
    }
}
