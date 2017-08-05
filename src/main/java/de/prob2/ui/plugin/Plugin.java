package de.prob2.ui.plugin;

import java.util.UUID;

public abstract class Plugin {

    private final int internalVersion;
    private final String version;
    private final String name;
    private final UUID uuid;

    protected Plugin (String name, String version, int internalVersion, UUID uuid) {
        this.name = name;
        this.version = version;
        this.internalVersion = internalVersion;
        this.uuid = uuid;
    }

    public abstract void start(PluginManager manager);

    public abstract void stop();

    public int getInternalVersion() {
        return internalVersion;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }
}
