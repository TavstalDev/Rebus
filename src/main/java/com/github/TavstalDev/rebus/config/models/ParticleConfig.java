package com.github.TavstalDev.rebus.config.models;

import com.github.TavstalDev.rebus.config.RebusConfig;
import lombok.Getter;

@Getter
public class ParticleConfig {
    private final String hightierA;
    private final String hightierB;

    public ParticleConfig(RebusConfig parent) {
        this.hightierA = parent.resolveGet("particles.hightier.a", "FIREWORK");
        this.hightierB = parent.resolveGet("particles.hightier.b", "FIREWORKS_SPARK");
    }
}
