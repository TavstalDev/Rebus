package com.github.TavstalDev.rebus.config;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.models.GeneralConfig;
import com.github.TavstalDev.rebus.config.models.NpcConfig;
import com.github.TavstalDev.rebus.config.models.ParticleConfig;
import com.github.TavstalDev.rebus.config.models.StorageConfig;
import com.github.TavstalDev.yggraLib.bootstrap.YggraConfig;

public class RebusConfig extends YggraConfig {
    public RebusConfig(Rebus plugin) {
        super(plugin, "config.yml", null);
    }

    /** General plugin settings (locale, prefix, economy, updates, debug). */
    public GeneralConfig general;
    /** Storage settings for database and Redis cache. */
    public StorageConfig storage;
    public NpcConfig npc;
    public ParticleConfig particle;

    @Override
    protected void loadDefaults() {
        general = new GeneralConfig(this);
        storage = new StorageConfig(this);
        npc = new NpcConfig(this);
        particle = new ParticleConfig(this);
    }
}
