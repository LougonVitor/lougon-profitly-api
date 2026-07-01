package tech.lougon.profitly.user.application.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.config.security.TokenService;
import tech.lougon.profitly.user.domain.model.User;
import tech.lougon.profitly.user.domain.repository.UserRepository;
import tech.lougon.profitly.user.infrastructure.security.UserSecurityDetails;
import tech.lougon.profitly.user.presentation.request.GoogleAuthRequest;
import tech.lougon.profitly.user.presentation.request.LoginRequest;
import tech.lougon.profitly.user.presentation.request.RegisterRequest;
import tech.lougon.profitly.user.presentation.response.AuthResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @Value("${google.client-id:}")
    private String googleClientId;

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
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Nome de usuário já em uso");
        }

        User user = new User(
                null, request.username(), request.email(),
                passwordEncoder.encode(request.password()),
                request.phone(), null,
                request.emailConsent(), request.smsConsent(),
                Instant.now()
        );
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

    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.credential());
            if (idToken == null) {
                throw new IllegalArgumentException("Token Google inválido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email    = payload.getEmail();
            String name     = (String) payload.get("name");

            User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
                // Try to link to existing account by email
                return userRepository.findByEmail(email).map(existing -> {
                    User linked = new User(
                            existing.id(), existing.username(), existing.email(),
                            existing.password(), existing.phone(), googleId,
                            existing.emailConsent(), existing.smsConsent(), existing.createdAt()
                    );
                    return userRepository.save(linked);
                }).orElseGet(() -> {
                    // Create new user from Google account
                    String username = generateUsername(email, name);
                    User newUser = new User(
                            null, username, email, null, null, googleId,
                            false, false, Instant.now()
                    );
                    return userRepository.save(newUser);
                });
            });

            String token = tokenService.generateTokenForUserId(user.id(), user.username());
            return new AuthResponse(token, user.username());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao autenticar com Google");
        }
    }

    private String generateUsername(String email, String name) {
        String base = name != null
                ? name.toLowerCase().replaceAll("[^a-z0-9]", "")
                : email.split("@")[0].replaceAll("[^a-z0-9]", "");
        if (base.length() < 3) base = "user" + base;
        String candidate = base;
        int i = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + i++;
        }
        return candidate;
    }
}
