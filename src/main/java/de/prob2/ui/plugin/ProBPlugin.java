package de.prob2.ui.plugin;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginManager;
import ro.fortsoft.pf4j.PluginWrapper;


public abstract class ProBPlugin extends Plugin{

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBPlugin.class);

    private boolean started = false;

    public ProBPlugin (PluginWrapper pluginWrapper) {
        super(pluginWrapper);
    }

    public abstract String getName();
    public abstract void startPlugin();
    public abstract void stopPlugin();

    @Override
    public final void start() throws PluginException {
        if (!started) {
            startPlugin();
            started = true;
        }
    }

    @Override
    public final void stop() throws PluginException {
        if (started) {
            stopPlugin();
            started = false;
        }
    }

    public ProBConnection getProBConnection() {
        ProBPluginManager pluginManager = getProBPluginManager();
        if (pluginManager != null) {
            return pluginManager.getProBConnection();
        }
        LOGGER.warn("Couldn't get ProBConnection!");
        return null;
    }

    public ProBPluginManager getProBPluginManager() {
        PluginManager pluginManager = getWrapper().getPluginManager();
        if (pluginManager instanceof ProBPluginManager) {
            return (ProBPluginManager) pluginManager;
        }
        LOGGER.warn("The PluginManager of plugin {} is not an instance of ProBPluginManager.", getName());
        return null;
    }

    public Injector getInjector() {
        return getProBConnection().getInjector();
    }

}
