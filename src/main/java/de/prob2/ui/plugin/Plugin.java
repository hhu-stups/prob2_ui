package de.prob2.ui.plugin;

import java.util.UUID;

public abstract class Plugin {

    private final int internalVersion;
    private final String version;
    private final String name;
    private final UUID uuid;

    boolean started = false;

    protected Plugin (String name, String version, int internalVersion, UUID uuid) {
        this.name = name;
        this.version = version;
        this.internalVersion = internalVersion;
        this.uuid = uuid;
    }

    public void start(PluginManager manager){
        if (!this.started) {
            this.safeStart(manager);
            this.started = true;
        }
    }

    public void stop(){
        if (this.started) {
            this.safeStop();
            this.started = false;
        }
    }

    protected abstract void safeStart(PluginManager manager);

    protected abstract void safeStop();

    public int getInternalVersion() {
        return this.internalVersion;
    }

    public String getVersion() {
        return this.version;
    }

    public String getName() {
        return this.name;
    }

    public UUID getUuid() {
        return this.uuid;
    }
}
