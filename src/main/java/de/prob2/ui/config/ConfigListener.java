package de.prob2.ui.config;

public interface ConfigListener {
	/**
	 * Load settings from the given config data and apply them to the UI.
	 * 
	 * When this listener is added using {@link Config#addListener(ConfigListener)}, this method is called once to make the listener aware of the current state of the config. Afterwards, this method is called every time the config is reloaded.
	 * 
	 * @param configData the loaded config data object from which the settings should be loaded
	 */
	void loadConfig(final ConfigData configData);
	
	/**
	 * Save the current state of the UI into the given config data object.
	 * 
	 * After this listener is added using {@link Config#addListener(ConfigListener)}, this method is called every time the config is saved.
	 * 
	 * @param configData the new config data object into which the settings should be saved
	 */
	void saveConfig(final ConfigData configData);
}
