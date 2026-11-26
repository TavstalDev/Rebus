package com.github.TavstalDev.rebus.config.models;

import com.github.TavstalDev.yggraLib.bootstrap.YggraConfig;
import lombok.Getter;

/**
 * Typed representation of the {@code general} configuration section.
 */
@Getter
public class GeneralConfig {
    /** Default locale identifier used by the plugin. */
    private final String locale;
    /** Whether player-specific locales should be used when available. */
    private final boolean usePlayerLocale;
    /** Whether update checks are enabled on startup. */
    private final boolean checkForUpdates;
    /** Whether debug logging is enabled. */
    private final boolean debug;
    /** Message prefix used by plugin messages. */
    private final String prefix;
    /** Whether Vault economy integration is enabled. */
    private final boolean useEconomy;

    /**
     * Loads general settings from the provided configuration.
     *
     * @param parent the parent configuration source
     */
    public GeneralConfig(YggraConfig parent) {
        locale = parent.resolveGet("general.locale", "eng");
        usePlayerLocale = parent.resolveGet("general.use-player-locale", true);
        checkForUpdates = parent.resolveGet("general.check-updates", true);
        debug = parent.resolveGet("general.debug-mode", false);
        prefix = parent.resolveGet("general.prefix", "&brebus &8»&7");
        useEconomy = parent.resolveGet("general.use-economy", false);
    }

    /**
     * Creates a general configuration model with explicit values.
     *
     * @param locale          default locale identifier
     * @param usePlayerLocale whether to use per-player locales
     * @param checkForUpdates whether update checks are enabled
     * @param debug           whether debug logging is enabled
     * @param prefix          message prefix
     * @param useEconomy      whether economy integration is enabled
     */
    public GeneralConfig(String locale, boolean usePlayerLocale, boolean checkForUpdates, boolean debug,
                         String prefix, boolean useEconomy) {
        this.locale = locale;
        this.usePlayerLocale = usePlayerLocale;
        this.checkForUpdates = checkForUpdates;
        this.debug = debug;
        this.prefix = prefix;
        this.useEconomy = useEconomy;
    }
}
