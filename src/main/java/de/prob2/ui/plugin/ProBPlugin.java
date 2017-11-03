package de.prob2.ui.plugin;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginManager;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 * This class will be extended by all plug-ins and
 * serves as the common class between a plug-in and the prob2-ui application.
 *
 * It extends the class {@link Plugin} of the PF4J framework and
 * adds abstract methods to start ({@code startPlugin}) and
 * stop ({@code stopPlugin}) the plug-in and
 * to give it a human readable name ({@code getName}).
 *
 * It also ensures that the {@code start} and {@code stop} method of the
 * {@link Plugin} class can only be called when the plug-in hast not yet been started
 * or stopped.
 *
 * @author  Christoph Heinzen
 * @version 0.1.0
 * @since   10.08.2017
 * @see ro.fortsoft.pf4j.Plugin
 */
public abstract class ProBPlugin extends Plugin{

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBPlugin.class);

    private boolean started = false;
    private final ProBPluginManager proBPluginManager;
    private final ProBPluginUIConnection proBPluginUIConnection;

    public ProBPlugin (PluginWrapper pluginWrapper, ProBPluginManager proBPluginManager, ProBPluginUIConnection proBPluginUIConnection) {
        super(pluginWrapper);
        this.proBPluginManager = proBPluginManager;
        this.proBPluginUIConnection = proBPluginUIConnection;
    }

    /**
     * Gives the plug-in a human readable name.
     *
     * @return name of the plugin
     */
    public abstract String getName();

    /**
     * Starts the plug-in and ensures that the {@code start} method of the plug-in is only called
     * when the plug-in has not yet been started.
     */
    public abstract void startPlugin();

    /**
     * Stops the plug-in and ensures that the {@code stop} method of the plug-in is only called
     * when the plug-in has not yet been stopped.
     */
    public abstract void stopPlugin();

    /**
     * {@inheritDoc}
     * @throws PluginException
     */
    @Override
    public final void start() throws PluginException {
        if (!started) {
            startPlugin();
            started = true;
        }
    }

    /**
     * {@inheritDoc}
     * @throws PluginException
     */
    @Override
    public final void stop() throws PluginException {
        if (started) {
            stopPlugin();
            started = false;
        }
    }

    /**
     * Getter for the singleton instance of the {@link ProBPluginUIConnection} of
     * the prob2-ui application. Uses the {@code getProBPluginUIConnection()} method of the
     * {@link ProBPluginManager}.
     *
     * @return singleton instance of the {@link ProBPluginUIConnection}
     */
    public ProBPluginUIConnection getProBPluginUIConnection() {
        return proBPluginUIConnection;
    }

    /**
     * Getter for the singleton instance of the {@link ProBPluginManager} of
     * the prob2-ui application.
     *
     * @return singleton instance of the {@link ProBPluginManager}
     */
    public ProBPluginManager getProBPluginManager() {
        return proBPluginManager;
    }

    /**
     * Getter for the {@link ProBPluginManager.ProBJarPluginManager} used to load this plug-in.
     *
     * @return Returns the {@link ProBPluginManager} used to load this plug-in.
     */
    public ProBPluginManager.ProBJarPluginManager getPluginManager() {
        PluginManager pluginManager = getWrapper().getPluginManager();
        if (pluginManager instanceof ProBPluginManager.ProBJarPluginManager) {
            return (ProBPluginManager.ProBJarPluginManager) pluginManager;
        }
        LOGGER.warn("The PluginManager of plugin {} is not an instance of ProBJarPluginManager.", getName());
        return null;
    }

    /**
     * Getter for the singleton instance of the Guice {@link Injector} of
     * the prob2-ui application.
     *
     * @return singleton instance of the {@link Injector}
     */
    public Injector getInjector() {
        return getProBPluginUIConnection().getInjector();
    }
}
