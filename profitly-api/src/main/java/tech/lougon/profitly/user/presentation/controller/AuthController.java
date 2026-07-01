package tech.lougon.profitly.user.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.user.application.service.UserService;
import tech.lougon.profitly.user.presentation.request.GoogleAuthRequest;
import tech.lougon.profitly.user.presentation.request.LoginRequest;
import tech.lougon.profitly.user.presentation.request.RegisterRequest;
import tech.lougon.profitly.user.presentation.response.AuthResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody @Valid GoogleAuthRequest request) {
        return ResponseEntity.ok(userService.loginWithGoogle(request));
    }
}
