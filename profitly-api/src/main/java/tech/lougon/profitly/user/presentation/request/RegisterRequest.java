package tech.lougon.profitly.user.presentation.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Nome de usuário é obrigatório")
        @Size(min = 3, max = 30, message = "Nome de usuário deve ter entre 3 e 30 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Nome de usuário deve conter apenas letras, números e _")
        String username,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\(\\d{2}\\)\\s?9?\\d{4}-?\\d{4}$", message = "Telefone inválido")
        String phone,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        @Pattern(regexp = ".*[A-Z].*", message = "Senha deve conter pelo menos uma letra maiúscula")
        @Pattern(regexp = ".*[0-9].*", message = "Senha deve conter pelo menos um número")
        String password,

        @NotBlank(message = "Confirmação de senha é obrigatória")
        String confirmPassword,

        boolean emailConsent,
        boolean smsConsent
) {}
