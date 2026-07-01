package tech.lougon.profitly.user.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String credential
) {}
