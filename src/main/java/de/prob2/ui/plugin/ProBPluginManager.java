package de.prob2.ui.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.internal.DefaultPluginDirectory;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.VersionInfo;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginException;
import org.pf4j.PluginFactory;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ProBPluginManager} is a wrapper for the {@link ProBJarPluginManager} which is an
 * implementation of the PF4J {@link org.pf4j.DefaultPluginManager}.
 *
 * {@link ProBPluginManager} has methods to start the plugin manager, to reload the plugins and
 * to add plugins using a {@link FileChooser}.
 */
@FXMLInjected
@Singleton
public class ProBPluginManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProBPluginManager.class);

	private static final Path OLD_DEFAULT_PLUGIN_DIRECTORY = Paths.get(Main.getProBDirectory(), "prob2ui", "plugins");

	private final ProBPluginHelper proBPluginHelper;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final Path defaultPluginDirectory;
	private final VersionInfo versionInfo;

	private List<String> inactivePluginIds;
	private Path pluginDirectory;
	private ProBJarPluginManager pluginManager;

	/**
	 * Should only be used by the Guice-Injector.
	 * Do not call this constructor.
	 *
	 * @param proBPluginHelper singleton instance of {@link ProBPluginHelper} used in the prob2-ui application
	 * @param stageManager singleton instance of {@link StageManager} used in the prob2-ui application
	 * @param bundle {@link ResourceBundle} used in the prob2-ui application
	 */
	@Inject
	public ProBPluginManager(ProBPluginHelper proBPluginHelper, StageManager stageManager, ResourceBundle bundle, final FileChooserManager fileChooserManager, @DefaultPluginDirectory final Path defaultPluginDirectory, final VersionInfo versionInfo, final StopActions stopActions, final Config config) {
		this.proBPluginHelper = proBPluginHelper;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		this.defaultPluginDirectory = defaultPluginDirectory;
		this.versionInfo = versionInfo;
		// Do not convert this to a method reference! Otherwise it won't work correctly if the plugin manager changes.
		stopActions.add(() -> this.getPluginManager().stopPlugins());
		// Adding the config listener immediately calls loadConfig,
		// which will create the plugin directory and initialize the plugin manager.
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				setPluginDirectory(configData.pluginDirectory);
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.pluginDirectory = getPluginDirectory();
			}
		});
	}

	/**
	 * Getter for the current {@link ProBJarPluginManager}, that the application is using.
	 *
	 * @return Return the current {@link ProBJarPluginManager}
	 */
	public ProBJarPluginManager getPluginManager() {
		return pluginManager;
	}

	/*
	 * methods to add functionality
	 */

	/**
	 * Shows a {@link FileChooser} to select a plugin.
	 * If the user selects a plugin, the selected file will be copied into to the plugins-directory.
	 * After that the plugin gets loaded and started if necessary.
	 * If an error occurs, an {@link Alert} with an error message will be shown.
	 *
	 */
	public void addPlugin() {
		//let the user select a plugin-file
		Stage stage = stageManager.getCurrent();
		final Path selectedPlugin = showFileChooser(stage);
		if (selectedPlugin != null) {
			Path pluginFileName = selectedPlugin.getFileName();
			Path plugin = getPluginDirectory().resolve(pluginFileName);
			try {
				createPluginDirectory();
				if (copyPluginFile(selectedPlugin, plugin)) {
					String pluginId = pluginManager.loadPlugin(plugin);
					if (checkLoadedPlugin(pluginId, pluginFileName)
						&& getInactivePluginIds() != null
						&& !getInactivePluginIds().contains(pluginId)) {
						pluginManager.startPlugin(pluginId);
					}
				}
			} catch (IOException | PluginException | RuntimeException e) {
				LOGGER.warn("Tried to copy and load/start the plugin {}.", pluginFileName, e);
				showWarningAlert("plugin.alerts.couldNotLoadPlugin.content", pluginFileName);
				//if an error occurred, delete the plugin file
				PluginWrapper wrapper = pluginManager.getPlugin(plugin);
				if (wrapper != null) {
					pluginManager.deletePlugin(wrapper.getPluginId());
				}
				try {
					Files.deleteIfExists(plugin);
				} catch (IOException ex) {
					LOGGER.warn("Failed to delete plugin", ex);
				}
			}
		}
	}

	/**
	 * This method loads all plugins in the plugins directory and
	 * starts the plugin if it is not marked as inactive in the {@code inactive.txt} file.
	 */
	public void start() {
		pluginManager.loadPlugins();
		for (PluginWrapper plugin : pluginManager.getPlugins()) {
			String pluginId = plugin.getPluginId();
			if (!getInactivePluginIds().contains(pluginId)) {
				pluginManager.startPlugin(pluginId);
			}
		}
	}

	/**
	 * This method unloads all loaded plugins and after that calls the
	 * {@code start} method of the {@link ProBPluginManager}.
	 */
	public void reloadPlugins() {
		List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();
		for (PluginWrapper plugin : loadedPlugins) {
			pluginManager.unloadPlugin(plugin.getPluginId());
		}
		start();
	}

	/**
	 * Changes the directory where PF4J searches for plugins.
	 * Because PF4J does not allow it to change the directory, we have
	 * to create a new instance of the {@link ProBJarPluginManager}.
	 *
	 * @return Returns the plugins in the new directory.
	 */
	List<PluginWrapper> changePluginDirectory() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle(bundle.getString("plugin.pluginMenu.directoryChooser.changePath.title"));
		chooser.setInitialDirectory(getPluginDirectory().toFile());
		final Path newPath = fileChooserManager.showDirectoryChooser(chooser, null, stageManager.getCurrent());
		if (newPath != null) {
			//unload all plugins
			List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();
			for (PluginWrapper plugin : loadedPlugins) {
				pluginManager.unloadPlugin(plugin.getPluginId());
			}
			//set new path
			pluginDirectory = newPath;
			//initialize the PluginManager using the new path
			pluginManager = new ProBJarPluginManager();
			//load an start the plugins
			start();
			return pluginManager.getPlugins();
		}
		return null;
	}

	/**
	 * Getter for the singleton instance of the {@link ProBPluginHelper} of
	 * the prob2-ui application.
	 *
	 * @return singleton instance of the {@link ProBPluginHelper}
	 */
	public ProBPluginHelper getProBPluginHelper() {
		return proBPluginHelper;
	}

	/**
	 * Saves the ids of every inactive plugin in the {@code inactive.txt} file.
	 * A plugin is inactive if its state is not {@code PluginState.STARTED}.
	 */
	void writeInactivePlugins() throws IOException {
		createPluginDirectory();
		Path inactivePlugins = getInactivePluginsFile();
		inactivePluginIds = pluginManager.getPlugins().stream()
				.filter(pluginWrapper -> pluginWrapper.getPluginState() != PluginState.STARTED)
				.map(PluginWrapper::getPluginId)
				.collect(Collectors.toList());
		Files.write(inactivePlugins, inactivePluginIds);
	}

	/*
	 * private methods used to add a new plugin
	 */

	private boolean copyPluginFile(Path source, Path destination) {
		if (destination.toFile().exists()) {
			//if there is already a file with the name, try to find the corresponding plugin
			PluginWrapper wrapper = pluginManager.getPlugin(destination);
			if (wrapper != null) {
				//if there is a corresponding plugin, ask the user if he wants to overwrite it
				List<ButtonType> buttons = new ArrayList<>();
				buttons.add(ButtonType.YES);
				buttons.add(ButtonType.NO);
				Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, Arrays.asList(ButtonType.YES, ButtonType.NO), "",
						"plugin.alerts.confirmOverwriteExistingFile.content", destination.getFileName(),
						((ProBPlugin) wrapper.getPlugin()).getName(), wrapper.getDescriptor().getVersion());
				Optional<ButtonType> result = alert.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.YES) {
					//if he wants to overwrite, delete the plugin
					pluginManager.deletePlugin(wrapper.getPluginId());
				} else {
					//if he doesn't want to overwrite it, do nothing
					return false;
				}
			} else {
				//if there is no corresponding plugin, delete the file
				try {
					Files.deleteIfExists(destination);
				} catch (IOException e) {
					LOGGER.warn("Failed to delete existing plugin file", e);
					showWarningAlert("plugin.alerts.couldNotDeleteJar.content", destination.getFileName());
					return false;
				}
			}
		}
		try {
			//copy the selected file into the plugins directory
			Files.copy(source, destination);
			return true;
		} catch (IOException e) {
			LOGGER.warn("Failed to copy plugin", e);
			showWarningAlert("plugin.alerts.couldNotCopyToPluginDirectory.content", destination.getFileName());
		}
		return false;
	}

	private boolean checkLoadedPlugin(String loadedPluginId, Path pluginFileName) throws PluginException {
		if (loadedPluginId == null) {
			// error while loading the plugin
			throw new PluginException("Could not load the plugin '{}'.", pluginFileName);
		} else {
			PluginWrapper pluginWrapper = pluginManager.getPlugin(loadedPluginId);
			if (pluginWrapper.getPlugin() instanceof InvalidPlugin) {
				InvalidPlugin invPlug = (InvalidPlugin) pluginWrapper.getPlugin();
				Alert alert;
				if (invPlug.getException() != null) {
					alert = stageManager.makeExceptionAlert(invPlug.getException(), invPlug.getMessageBundleKey(), invPlug.getPluginClassName());
				} else {
					alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", invPlug.getMessageBundleKey(),
							invPlug.getPluginClassName());
				}
				alert.initOwner(stageManager.getCurrent());
				alert.initModality(Modality.APPLICATION_MODAL);
				alert.show();
			}
			//because we don't use the enabled/disabled.txt of PF4J, the only reason for a
			//plugin to be disabled is, when it has the wrong version
			if (pluginWrapper.getPluginState() == PluginState.DISABLED) {
				showWarningAlert("plugin.alerts.wrongUIversion.content",
						pluginWrapper.getPluginPath().getFileName(),
						pluginWrapper.getDescriptor().getRequires(),
						pluginManager.getSystemVersion());
				pluginManager.deletePlugin(loadedPluginId);
				return false;
			}
		}
		return true;
	}

	private Path showFileChooser(@Nonnull final Stage stage) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("plugin.fileChooser.addPlugin.title"));
		fileChooser.getExtensionFilters()
				.addAll(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Plugin", "jar"));
		return fileChooserManager.showOpenFileChooser(fileChooser, Kind.PLUGINS, stage);
	}

	/*
	 * methods to handle the inactive plugins
	 */

	private void readInactivePlugins() throws IOException {
		try {
			inactivePluginIds = Files.readAllLines(getInactivePluginsFile());
		} catch (FileNotFoundException | NoSuchFileException e) {
			LOGGER.info("Inactive plugins list not found, all plugins will be loaded");
			inactivePluginIds = Collections.emptyList();
		}
	}

	private List<String> getInactivePluginIds() {
		if (inactivePluginIds == null) {
			try {
				readInactivePlugins();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return inactivePluginIds;
	}

	private Path getInactivePluginsFile() {
		return getPluginDirectory().resolve("inactive.txt");
	}

	/*
	 * methods to handle the plugin directory
	 */

	private Path migrateOldPluginDirectoryIfNeeded(final Path pluginDirectoryInConfig) throws IOException {
		if (!Files.exists(defaultPluginDirectory)) {
			if (Files.exists(OLD_DEFAULT_PLUGIN_DIRECTORY)) {
				LOGGER.info("Found old plugin directory at {} - migrating to {}", OLD_DEFAULT_PLUGIN_DIRECTORY, defaultPluginDirectory);
				Files.createDirectories(defaultPluginDirectory);
				// Copy old plugin directory (recursively) to new location
				Files.walkFileTree(OLD_DEFAULT_PLUGIN_DIRECTORY, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
						Files.createDirectories(defaultPluginDirectory.resolve(OLD_DEFAULT_PLUGIN_DIRECTORY.relativize(dir)));
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
						Files.copy(file, defaultPluginDirectory.resolve(OLD_DEFAULT_PLUGIN_DIRECTORY.relativize(file)));
						return FileVisitResult.CONTINUE;
					}
				});
			}
			if (OLD_DEFAULT_PLUGIN_DIRECTORY.equals(pluginDirectoryInConfig)) {
				LOGGER.info("Plugin directory in config was set to old default plugin directory ({}) - replacing with new default plugin directory ({})", OLD_DEFAULT_PLUGIN_DIRECTORY, defaultPluginDirectory);
				return defaultPluginDirectory;
			}
		}
		return pluginDirectoryInConfig;
	}

	private void createPluginDirectory() throws IOException {
		Files.createDirectories(getPluginDirectory());
	}

	public Path getPluginDirectory() {
		if (pluginDirectory != null) {
			return pluginDirectory;
		}
		return defaultPluginDirectory;
	}

	/**
	 * Do not call this method.
	 */
	public void setPluginDirectory(final Path pathInConfig) {
		Path path = pathInConfig;
		if (this.pluginDirectory == null) {
			try {
				path = this.migrateOldPluginDirectoryIfNeeded(pathInConfig);
			} catch (IOException e) {
				LOGGER.error("Failed to migrate old plugin directory - ignoring", e);
			}
		}
		if (path != null) {
			this.pluginDirectory = path;
		}
		if (path != null || pluginManager == null) {
			// If the path was changed or the plugin manager hasn't been initialized yet,
			// create the plugin directory (if necessary) and (re)initialize the plugin manager.
			try {
				createPluginDirectory();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			//initialize with the new PluginDirectory
			pluginManager = new ProBJarPluginManager();
		}
	}

	/*
	 * GUI helper methods
	 */

	private void showWarningAlert(String bundleKey, Object... stringParams) {
		Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", bundleKey, stringParams);
		alert.initOwner(stageManager.getCurrent());
		alert.show();
	}

	/**
	 * Slightly changed version of the PF4J-{@link org.pf4j.DefaultPluginManager}
	 *
	 * {@inheritDoc}
	 *
	 * Overwrites the {@code createPluginFactory} method to use {@link ProBPlugin} as plugin clazz,
	 * the {@code createPluginsRoot} method to set the plugins directory and the {@code getRuntimeMode}
	 * to avoid the development mode of PF4J.
	 *
	 * @see org.pf4j.DefaultPluginManager
	 * @see org.pf4j.AbstractPluginManager
	 */
	public class ProBJarPluginManager extends DefaultPluginManager {

		private ProBJarPluginManager(){
			setSystemVersion(versionInfo.getUIVersion());
			setExactVersionAllowed(true);
		}

		@Override
		protected Path createPluginsRoot() {
			try {
				createPluginDirectory();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return getPluginDirectory();
		}

		@Override
		//changed to use the ProBPlugin clazz
		protected PluginFactory createPluginFactory() {
			return pluginWrapper -> {
				String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
				LOGGER.debug("Create instance for plugin '{}'", pluginClassName);

				Class<?> pluginClass;
				try {
					pluginClass = pluginWrapper.getPluginClassLoader().loadClass(pluginClassName);
				} catch (ClassNotFoundException e) {
					LOGGER.error(e.getMessage(), e);
					return new InvalidPlugin(pluginWrapper, "plugin.invalidPlugin.message.couldNotFindPluginClass",
							pluginClassName, e);
				}

				// once we have the clazz, we can do some checks on it to ensure
				// that it is a valid implementation of a plugin.
				int modifiers = pluginClass.getModifiers();
				if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)
						|| (!ProBPlugin.class.isAssignableFrom(pluginClass))) {
					LOGGER.error("The plugin class '{}' is not a valid ProBPlugin", pluginClassName);
					return new InvalidPlugin(pluginWrapper,
							"plugin.invalidPlugin.message.notAValidPluginClass", pluginClassName);
				}

				// create the ProBPlugin instance
				try {
					Constructor<?> constructor =
							pluginClass.getConstructor(PluginWrapper.class, ProBPluginManager.class, ProBPluginHelper.class);
					return (ProBPlugin) constructor.newInstance(pluginWrapper, ProBPluginManager.this, proBPluginHelper);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					return new InvalidPlugin(pluginWrapper, "plugin.invalidPlugin.message.couldNotCreateInstance",
							pluginClassName, e);

				}
			};
		}

		@Override
		public RuntimeMode getRuntimeMode() {
			return RuntimeMode.DEPLOYMENT;
		}

		@Override
		//also checked required version
		protected void validatePluginDescriptor(PluginDescriptor descriptor) throws PluginException {
			//TODO: show what is wrong in an alert
			super.validatePluginDescriptor(descriptor);
			if (descriptor.getRequires() == null || descriptor.getRequires().isEmpty() || descriptor.getRequires().equals("*")) {
				throw new PluginException("Plugin-Requires has to be specified!");
			}
			if (!descriptor.getDependencies().isEmpty()) {
				StringBuilder builder = new StringBuilder("Plugin-Dependencies are not supported but the plugin has the following dependencies:");
				for (PluginDependency dependency : descriptor.getDependencies()) {
					builder.append(System.getProperty("line.separator"));
					builder.append(dependency.getPluginId());
				}
				throw new PluginException(builder.toString());
			}
		}

		private PluginWrapper getPlugin(Path pluginPath) {
			for (PluginWrapper pluginWrapper : getPlugins()) {
				if (pluginWrapper.getPluginPath().equals(pluginPath)) {
					return pluginWrapper;
				}
			}
			return null;
		}
	}
}
