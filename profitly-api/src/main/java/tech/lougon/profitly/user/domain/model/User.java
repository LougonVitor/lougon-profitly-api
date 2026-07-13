package tech.lougon.profitly.user.domain.model;

import java.time.Instant;

public record User(
        String id,
        String username,
        String email,
        String googleId,
        Instant createdAt
) {}
