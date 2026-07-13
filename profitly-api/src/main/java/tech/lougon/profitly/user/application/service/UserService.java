package tech.lougon.profitly.user.application.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.config.security.TokenService;
import tech.lougon.profitly.user.domain.model.User;
import tech.lougon.profitly.user.domain.repository.UserRepository;
import tech.lougon.profitly.user.presentation.request.GoogleAuthRequest;
import tech.lougon.profitly.user.presentation.response.AuthResponse;

import java.time.Instant;
import java.util.Collections;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Value("${google.client-id:}")
    private String googleClientId;

    public UserService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
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
                            googleId, existing.createdAt()
                    );
                    return userRepository.save(linked);
                }).orElseGet(() -> {
                    // Create new user from Google account
                    String username = generateUsername(email, name);
                    User newUser = new User(
                            null, username, email, googleId, Instant.now()
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
