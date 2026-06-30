package tech.lougon.profitly.user.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.config.security.TokenService;
import tech.lougon.profitly.user.domain.model.User;
import tech.lougon.profitly.user.domain.repository.UserRepository;
import tech.lougon.profitly.user.infrastructure.security.UserSecurityDetails;
import tech.lougon.profitly.user.presentation.request.LoginRequest;
import tech.lougon.profitly.user.presentation.request.RegisterRequest;
import tech.lougon.profitly.user.presentation.response.AuthResponse;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User(null, request.username(), request.email(),
                passwordEncoder.encode(request.password()), Instant.now());
        userRepository.save(user);

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = tokenService.generateToken((UserSecurityDetails) auth.getPrincipal());
        return new AuthResponse(token, request.username());
    }

    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserSecurityDetails userDetails = (UserSecurityDetails) auth.getPrincipal();
        String token = tokenService.generateToken(userDetails);
        return new AuthResponse(token, userDetails.getUsername());
    }
}
