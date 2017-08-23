package de.prob2.ui.plugin;

import com.github.zafarkhaja.semver.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.Main;
import de.prob2.ui.internal.StageManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.*;
import ro.fortsoft.pf4j.util.FileUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The {@link ProBPluginManager} is a wrapper for the {@link ProBJarPluginManager} which is an
 * implementation of the PF4J {@link JarPluginManager}.
 *
 * {@link ProBPluginManager} has methods to start the plugin manager, to reload the plugins and
 * to add plugins using a {@link FileChooser}.
 *
 * @author  Christoph Heinzen
 * @version 0.1.0
 * @since   10.08.2017
 */
@Singleton
public class ProBPluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBPluginManager.class);

    private static final String VERSION = "0.1.0";
    private static final File PLUGIN_DIRECTORY = new File(Main.getProBDirectory()
            + File.separator + "prob2ui"
            + File.separator + "plugins");

    private final ProBConnection proBConnection;
    private final StageManager stageManager;
    private final ResourceBundle bundle;

    private List<String> inactivePluginIds;
    private File pluginDirectory;
    private ProBJarPluginManager pluginManager;

    /**
     * Should only be used by the Guice-Injector.
     * Do not call this constructor.
     *
     * @param proBConnection singleton instance of {@link ProBConnection} used in the prob2-ui application
     * @param stageManager singleton instance of {@link StageManager} used in the prob2-ui application
     * @param bundle {@link ResourceBundle} used in the prob2-ui application
     */
    @Inject
    public ProBPluginManager(ProBConnection proBConnection, StageManager stageManager, ResourceBundle bundle) {
        this.proBConnection = proBConnection;
        this.stageManager = stageManager;
        this.bundle = bundle;
        this.pluginManager = new ProBJarPluginManager();
        createPluginDirectory();
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
     * Calls the {@code addPlugin(Stage stage)} method with the main-stage provided by the
     * {@link StageManager}.
     */
    public void addPlugin() {
        addPlugin(stageManager.getMainStage());
    }

    /**
     * Shows a {@link FileChooser} to select a plugin.
     * If the user selects a plugin, the selected file will be copied into to the plugins-directory.
     * After that the plugin gets loaded and started if necessary.
     * If an error occurs, an {@link Alert} with an error message will be shown.
     *
     * @param stage parent-stage of the used {@link FileChooser}
     */
    void addPlugin(@Nonnull final Stage stage) {
        //let the user select a plugin-file
        final File selectedPlugin = showFileChooser(stage);
        if (selectedPlugin != null && createPluginDirectory()) {
            String pluginFileName = selectedPlugin.getName();
            File plugin = new File(getPluginDirectory() + File.separator + pluginFileName);
            try {
                if (copyPluginFile(selectedPlugin, plugin, stage)) {
                    String pluginId = pluginManager.loadPlugin(plugin.toPath());
                    if (checkLoadedPlugin(stage, pluginId, pluginFileName)
                            && getInactivePluginIds() != null
                            && !getInactivePluginIds().contains(pluginId)) {
                        pluginManager.startPlugin(pluginId);
                    }
                }
            } catch (PluginException e) {
                LOGGER.warn("Tried to copy and load/start the plugin {}.\nThis exception was thrown: ", pluginFileName, e);
                showWarningAlert(stage, "plugins.error.load", pluginFileName);
                //if an error occurred, delete the plugin file
                try {
                    Files.deleteIfExists(plugin.toPath());
                } catch (IOException ex) {
                    LOGGER.warn("Could not delete file " + pluginFileName + ".");
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
        List<String> inactivePluginIds = getInactivePluginIds();
        if (inactivePluginIds != null) {
            for (PluginWrapper plugin : pluginManager.getPlugins()) {
                String pluginId = plugin.getPluginId();
                if (!inactivePluginIds.contains(pluginId)) {
                    pluginManager.startPlugin(pluginId);
                }
            }
        } else {
            showWarningAlert(stageManager.getMainStage(), "plugins.error.inactive");
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
     * Because PF4J does not allow it to change thee directory, we have
     * to create a new instance of the {@link ProBJarPluginManager}.
     *
     * @param stage The owner window of the used {@link DirectoryChooser}.
     * @return Returns the plugins in the new directory.
     */
    List<PluginWrapper> changePluginDirectory(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(bundle.getString("pluginsmenu.changepath"));
        chooser.setInitialDirectory(getPluginDirectory());
        File newPath = chooser.showDialog(stage);
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
     * Getter for the singleton instance of the {@link ProBConnection} of
     * the prob2-ui application.
     *
     * @return singleton instance of the {@link ProBConnection}
     */
    public ProBConnection getProBConnection() {
        return proBConnection;
    }

    /**
     * Saves the ids of every inactive plugin in the {@code inactive.txt} file.
     * A plugin is inactive if its state is not {@code PluginState.STARTED}.
     */
    void writeInactivePlugins() {
        try {
            if (createPluginDirectory()) {
                File inactivePlugins = getInactivePluginsFile();
                if (!inactivePlugins.exists()) {
                    if (!inactivePlugins.createNewFile()) {
                        LOGGER.warn("Could not create file for inactive plugins!");
                        return;
                    }
                }
                inactivePluginIds = pluginManager.getPlugins().stream()
                        .filter(pluginWrapper -> pluginWrapper.getPluginState() != PluginState.STARTED)
                        .map(PluginWrapper::getPluginId)
                        .collect(Collectors.toList());
                FileUtils.writeLines(inactivePluginIds, inactivePlugins);
            }
        } catch (IOException e) {
            LOGGER.warn("An error occurred while writing the inactive plugins:", e);
        }
    }

    /*
     * private methods used to add a new plugin
     */

    private boolean copyPluginFile(File source, File destination, Stage parentStage) {
        if (destination.exists()) {
            //if there is already a file with the name, try to find the corresponding plugin
            PluginWrapper wrapper = pluginManager.getPlugin(destination.toPath());
            if (wrapper != null) {
                //if there is a corresponding plugin, ask the user if he wants to overwrite it
                Alert dialog = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
                        String.format(bundle.getString("plugins.confirmation.copy"),
                                destination.getName(),
                                ((ProBPlugin) wrapper.getPlugin()).getName(),
                                wrapper.getDescriptor().getVersion()),
                        ButtonType.YES, ButtonType.NO);
                dialog.initOwner(stageManager.getMainStage());
                Optional<ButtonType> result = dialog.showAndWait();
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
                    Files.deleteIfExists(destination.toPath());
                } catch (IOException ex) {
                    LOGGER.warn("Could not delete file " + destination.getName() + ".");
                    showWarningAlert(parentStage, "plugins.error.delete", destination.getName());
                    return false;
                }
            }
        }
        try {
            //copy the selected file into the plugins directory
            Files.copy(source.toPath(), destination.toPath());
            return true;
        } catch (IOException e) {
            showWarningAlert(parentStage,"plugins.error.copy", destination.getName());
        }
        return false;
    }

    private boolean checkLoadedPlugin(Stage parentStage, String loadedPluginId, String pluginFileName) throws PluginException {
        if (loadedPluginId == null) {
            // error while loading the plugin
            throw new PluginException("Could not load the plugin '" + pluginFileName + "'.");
        } else {
            PluginWrapper pluginWrapper = pluginManager.getPlugin(loadedPluginId);
            //because we don't use the enabled/disabled.txt of PF4J, the only reason for a
            //plugin to be disabled is, when it has the wrong version
            if (pluginWrapper.getPluginState() == PluginState.DISABLED) {
                showWarningAlert(parentStage,
                        "plugins.error.version",
                        pluginWrapper.getPluginPath().getFileName(),
                        pluginWrapper.getDescriptor().getRequires(),
                        pluginManager.getSystemVersion());
                pluginManager.deletePlugin(loadedPluginId);
                return false;
            }
        }
        return true;
    }

    private File showFileChooser(@Nonnull final Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("menu.plugin.items.add"));
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("ProB2 Plugins", "*.jar"));
        return fileChooser.showOpenDialog(stage);
    }

    /*
     * methods to handle the inactive plugins
     */

    private void readInactivePlugins() {
        try {
            File inactivePlugins = getInactivePluginsFile();
            if (inactivePlugins.exists()) {
                //if we already have a incative plugins file, read it
                inactivePluginIds = FileUtils.readLines(inactivePlugins, true);
            } else {
                //if not, try to create an empty file
                if (!createPluginDirectory() || !inactivePlugins.createNewFile() ) {
                    LOGGER.warn("Could not create file for inactive plugins!");
                }
                inactivePluginIds = null;
            }
        } catch (IOException e) {
            LOGGER.warn("An error occurred while reading the inactive plugins:", e);
        }
    }

    private List<String> getInactivePluginIds() {
        if (inactivePluginIds == null) {
            readInactivePlugins();
        }
        return inactivePluginIds;
    }

    private File getInactivePluginsFile() {
        return new File(getPluginDirectory() + File.separator + "inactive.txt");
    }

    /*
     * methods to handle the plugin directory
     */

    private boolean createPluginDirectory() {
        File directory = getPluginDirectory();
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                LOGGER.warn("Couldn't create the directory for plugins!\n{}", directory.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public File getPluginDirectory() {
        if (pluginDirectory != null) {
            return pluginDirectory;
        }
        return PLUGIN_DIRECTORY;
    }

    /**
     * Do not call this method.
     */
    public void setPluginDirectory(String path) {
        if (path != null) {
            this.pluginDirectory = new File(path);
            //initialize with the new PluginDirectory
            pluginManager = new ProBJarPluginManager();
        }
    }

    /*
     * GUI helper methods
     */

    private void showWarningAlert(Stage parentStage, String bundleKey, Object... stringParams) {
        Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
                String.format(bundle.getString(bundleKey), stringParams),
                ButtonType.OK);
        alert.initOwner(parentStage);
        alert.show();
    }

    /**
     * Slightly changed version of the PF4J-{@link JarPluginManager}
     *
     * {@inheritDoc}
     *
     * Overwrites the {@code createPluginFactory} method to use {@link ProBPlugin} as plugin class,
     * the {@code createPluginsRoot} method to set the plugins directory and the {@code getRuntimeMode}
     * to avoid the development mode of PF4J.
     *
     * @author  Christoph Heinzen
     * @version 0.1.0
     * @since   23.08.2017
     */
    public class ProBJarPluginManager extends JarPluginManager {

        private ProBJarPluginManager(){
            setSystemVersion(Version.valueOf(VERSION));
            setExactVersionAllowed(true);
        }

        @Override
        protected Path createPluginsRoot() {
            if (pluginDirectory != null) {
                if (createPluginDirectory()) {
                    return pluginDirectory.toPath();
                }
                LOGGER.warn("Couldn't create plugin directory {}. Using the default directory {}.",
                        pluginDirectory, PLUGIN_DIRECTORY);
            }
            return PLUGIN_DIRECTORY.toPath();
        }

        @Override
        //changed to use the ProBPlugin class
        protected PluginFactory createPluginFactory() {
            return pluginWrapper -> {
                String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
                LOGGER.debug("Create instance for plugin '{}'", pluginClassName);

                Class<?> pluginClass;
                try {
                    pluginClass = pluginWrapper.getPluginClassLoader().loadClass(pluginClassName);
                } catch (ClassNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                    return null;
                }

                // once we have the class, we can do some checks on it to ensure
                // that it is a valid implementation of a plugin.
                int modifiers = pluginClass.getModifiers();
                if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)
                        || (!ProBPlugin.class.isAssignableFrom(pluginClass))) {
                    LOGGER.error("The plugin class '{}' is not a valid ProBPlugin", pluginClassName);
                    return null;
                }

                // create the ProBPlugin instance
                try {
                    Constructor<?> constructor = pluginClass.getConstructor(PluginWrapper.class);
                    return (ProBPlugin) constructor.newInstance(pluginWrapper);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return null;
            };
        }

        @Override
        public RuntimeMode getRuntimeMode() {
            return RuntimeMode.DEPLOYMENT;
        }

        public ProBPluginManager getPluginManager() {
            return ProBPluginManager.this;
        }

        private PluginWrapper getPlugin(Path pluginPath) {
            for (PluginWrapper pluginWrapper : pluginManager.getPlugins()) {
                if (pluginWrapper.getPluginPath().equals(pluginPath)) {
                    return pluginWrapper;
                }
            }
            return null;
        }
    }
}
