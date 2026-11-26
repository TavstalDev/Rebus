package com.github.TavstalDev.rebus.models;

import lombok.Getter;

@Getter
public class SoundData {
    private final String name;
    private final float volume;
    private final float pitch;

    public SoundData(String name, float volume, float pitch) {
        this.name = name;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SoundData(String name) {
        this(name, 1.0f, 1.0f);
    }
}
