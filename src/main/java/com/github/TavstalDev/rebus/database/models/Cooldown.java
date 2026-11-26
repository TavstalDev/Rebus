package com.github.TavstalDev.rebus.database.models;

import com.github.TavstalDev.yggraLib.api.database.annotations.Column;
import com.github.TavstalDev.yggraLib.api.database.annotations.Table;
import com.github.TavstalDev.yggraLib.api.database.enums.EColumnKind;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a cooldown for a specific chest in a given context.
 */
@Table("cooldowns")
@Getter
public class Cooldown {

    @Column(value = "id", kind = EColumnKind.PRIMARY)
    private UUID id;

    @Column(value = "playerId", nullable = false)
    private UUID playerId;

    @Column(value = "context", nullable = false)
    private String context;

    @Column(value = "type", nullable = false)
    private ECooldownType type;

    @Column(value = "chest", nullable = false)
    private String chest;

    @Column(value = "expiresAt", nullable = false)
    private LocalDateTime expiresAt;

    public Cooldown() {
    }

    public Cooldown(UUID id, UUID playerId, String context, ECooldownType type, String chest, LocalDateTime expiresAt) {
        this.id = id;
        this.playerId = playerId;
        this.context = context;
        this.type = type;
        this.chest = chest;
        this.expiresAt = expiresAt;
    }

    /**
     * Checks if the cooldown has expired.
     *
     * @return True if the current time is after the expiration time, false otherwise.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
