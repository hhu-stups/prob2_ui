package de.prob2.ui.plugin;

import com.google.inject.Injector;

import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @see org.pf4j.Plugin
 */
public abstract class ProBPlugin extends Plugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProBPlugin.class);

	private boolean started = false;
	private final ProBPluginManager proBPluginManager;
	private final ProBPluginHelper proBPluginHelper;

	public ProBPlugin (PluginWrapper pluginWrapper, ProBPluginManager proBPluginManager, ProBPluginHelper proBPluginHelper) {
		super(pluginWrapper);
		this.proBPluginManager = proBPluginManager;
		this.proBPluginHelper = proBPluginHelper;
	}

	/**
	 * Gives the plug-in a human readable name.
	 *
	 * @return name of the plugin
	 */
	public abstract String getName();

	/**
	 * Starts the plug-in and ensures that the {@code start} method of the plug-in is only called
	 * when the plug-in has not yet been started. If an exception gets thrown during the method the
	 * {@code stopPlugin} method will be called.
	 */
	public abstract void startPlugin() throws Exception;

	/**
	 * Stops the plug-in and ensures that the {@code stop} method of the plug-in is only called
	 * when the plug-in has not yet been stopped. The stop method is also called when the {@code startPlugin}
	 * method throws an exception so that this method should be prepared for that.
	 */
	public abstract void stopPlugin() throws Exception;

	@Override
	public final void start() {
		if (!started) {
			try {
				startPlugin();
				started = true;
			} catch (Exception ex) {
				LOGGER.warn("Exception while starting the plug-in {}", getName(), ex);
				getProBPluginHelper().getStageManager()
						.makeExceptionAlert(ex, "plugin.alerts.couldNotStartPlugin.content", getName()).show();
			}
		}
	}

	@Override
	public final void stop() {
		if (started) {
			try {
				stopPlugin();
				started = false;
			} catch (Exception ex) {
				LOGGER.warn("Exception while stopping the plug-in {}", getName(), ex);
				getProBPluginHelper().getStageManager()
						.makeExceptionAlert(ex, "plugin.alerts.couldNotStopPlugin.content", getName()).show();
			}
		}
	}

	/**
	 * Getter for the singleton instance of the {@link ProBPluginHelper} of
	 * the prob2-ui application. Uses the {@code getProBPluginHelper()} method of the
	 * {@link ProBPluginManager}.
	 *
	 * @return singleton instance of the {@link ProBPluginHelper}
	 */
	public ProBPluginHelper getProBPluginHelper() {
		return proBPluginHelper;
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
		return getProBPluginHelper().getInjector();
	}
}
