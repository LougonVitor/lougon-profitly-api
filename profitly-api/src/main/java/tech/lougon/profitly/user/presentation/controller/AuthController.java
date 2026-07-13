package tech.lougon.profitly.user.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.user.application.service.UserService;
import tech.lougon.profitly.user.presentation.request.GoogleAuthRequest;
import tech.lougon.profitly.user.presentation.response.AuthResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody @Valid GoogleAuthRequest request) {
        return ResponseEntity.ok(userService.loginWithGoogle(request));
    }
}
