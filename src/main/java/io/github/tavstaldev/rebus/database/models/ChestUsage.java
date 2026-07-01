package io.github.tavstaldev.rebus.database.models;

import io.github.tavstaldev.yggra.core.database.annotations.Column;
import io.github.tavstaldev.yggra.core.database.annotations.Table;
import io.github.tavstaldev.yggra.core.database.enums.EColumnKind;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("chest_usage")
@Getter
public class ChestUsage {

    @Column(value = "id", kind = EColumnKind.PRIMARY)
    private UUID id;

    @Column(value = "playerId", nullable = false)
    private UUID playerId;

    @Column(value = "context", nullable = false)
    private String context;

    @Column(value = "chest", nullable = false)
    private String chest;

    @Column(value = "usages", nullable = false)
    private int usages;

    @Column(value = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    public ChestUsage() {

    }

    public ChestUsage(UUID id, UUID playerId, String context, String chest, int usages, LocalDateTime updatedAt) {
        this.id = id;
        this.playerId = playerId;
        this.context = context;
        this.chest = chest;
        this.usages = usages;
        this.updatedAt = updatedAt;
    }
    public ChestUsage(UUID id, UUID playerId, String context, String chest, int usages) {
        this(id, playerId, context, chest, usages, LocalDateTime.now());
    }

    public void updateUsages(int newValue) {
        this.usages = newValue;
    }
}
