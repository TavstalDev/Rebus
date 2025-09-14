package io.github.tavstaldev.rebus.models;

import java.time.LocalDateTime;

public class Cooldown {
    private final String context;
    private final String chest;
    private final LocalDateTime expiresAt;

    public Cooldown(String context, String chest, LocalDateTime expiresAt) {
        this.context = context;
        this.chest = chest;
        this.expiresAt = expiresAt;
    }

    public String getContext() {
        return context;
    }

    public String getChest() {
        return chest;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
