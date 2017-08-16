package de.prob2.ui.plugin;

import com.github.zafarkhaja.semver.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.*;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Christoph Heinzen on 10.08.17.
 */
@Singleton
public class ProBPluginManager extends JarPluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBPluginManager.class);

    private static final File PLUGIN_DIRECTORY;
    private static final File INACTIVE_PLUGINS_FILE;

    static {
        String pluginDirectorPath = System.getProperty("user.home")
                + File.separator + ".prob"
                + File.separator + "prob2"
                + File.separator + "prob2ui"
                + File.separator + "plugins";

        PLUGIN_DIRECTORY = new File(pluginDirectorPath);
        INACTIVE_PLUGINS_FILE = new File(pluginDirectorPath + File.separator + "inactive.txt");
    }

    private final ProBConnection proBConnection;
    private final StageManager stageManager;
    private final ResourceBundle bundle;
    private List<String> inactivePluginIds;

    @Inject
    public ProBPluginManager(ProBConnection proBConnection, StageManager stageManager, ResourceBundle bundle) {
        this.proBConnection = proBConnection;
        this.stageManager = stageManager;
        this.bundle = bundle;
        setSystemVersion(Version.valueOf("0.1.0"));
        setExactVersionAllowed(true);
        createPluginDirectory();
    }

    @Override
    protected Path createPluginsRoot() {
        return PLUGIN_DIRECTORY.toPath();
    }

    @Override
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

            // create the plugin instance
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

    public void addPlugin() {
        addPlugin(stageManager.getMainStage());
    }

    public void addPlugin(@Nonnull final Stage stage) {
        final File selectedPlugin = showFileChooser(stage);
        if (selectedPlugin != null && createPluginDirectory()) {
            String pluginFileName = selectedPlugin.getName();
            File plugin = new File(PLUGIN_DIRECTORY + File.separator + pluginFileName);
            try {
                Files.copy(selectedPlugin.toPath(), plugin.toPath());
                String pluginId = loadPlugin(plugin.toPath());
                if (checkLoadedPlugin(pluginId, pluginFileName) && !inactivePluginIds.contains(pluginId)) {
                    startPlugin(pluginId);
                }
            } catch (Exception e) {
                LOGGER.warn("Tried to copy and load/start the plugin {}.\nThis exception was thrown: ", pluginFileName, e);
                if (e instanceof PluginException) {
                    showWarningAlert("plugins.error.load", pluginFileName);
                } else {
                    showWarningAlert("plugins.error.copy", pluginFileName);
                }
                //if an error occurred, delete the plugin
                try {
                    Files.deleteIfExists(plugin.toPath());
                } catch (IOException ex) {
                    LOGGER.warn("Could not delete file " + pluginFileName + ".");
                }
            }
        }
    }

    private boolean checkLoadedPlugin(String loadedPluginId, String pluginFileName) throws PluginException {
        if (loadedPluginId == null) {
            throw new PluginException("Could not load the plugin '" + pluginFileName + "'.");
        } else {
            PluginWrapper pluginWrapper = getPlugin(loadedPluginId);
            if (pluginWrapper.getPluginState() == PluginState.DISABLED) {
                showWarningAlert("plugins.error.version",
                        pluginWrapper.getPluginPath().getFileName(),
                        pluginWrapper.getDescriptor().getRequires(),
                        getSystemVersion());
                deletePlugin(loadedPluginId);
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

    public void start() {
        loadPlugins();
        List<String> inactivePluginIds = getInactivePluginIds();
        if (inactivePluginIds != null) {
            for (PluginWrapper plugin : getPlugins()) {
                String pluginId = plugin.getPluginId();
                if (!inactivePluginIds.contains(pluginId)) {
                    startPlugin(pluginId);
                }
            }
        } else {
            showWarningAlert("plugins.error.inactive");
        }
    }

    public List<PluginWrapper> reloadPlugins() {
        List<PluginWrapper> loadedPlugins = getPlugins();
        for (PluginWrapper plugin : loadedPlugins) {
            unloadPlugin(plugin.getPluginId());
        }
        start();
        return getPlugins();
    }

    public ProBConnection getProBConnection() {
        return proBConnection;
    }

    public void writeInactivePlugins() {
        try {
            if (createPluginDirectory()) {
                if (!INACTIVE_PLUGINS_FILE.exists()) {
                    if (!INACTIVE_PLUGINS_FILE.createNewFile()) {
                        LOGGER.warn("Could not create file for inactive plugins!");
                        return;
                    }
                }
                inactivePluginIds = getPlugins().stream()
                        .filter(pluginWrapper -> pluginWrapper.getPluginState() != PluginState.STARTED)
                        .map(PluginWrapper::getPluginId)
                        .collect(Collectors.toList());
                FileUtils.writeLines(inactivePluginIds, INACTIVE_PLUGINS_FILE);
            }
        } catch (IOException e) {
            LOGGER.warn("An error occurred while writing the inactive plugins:", e);
        }
    }

    private void readInactivePlugins() {
        try {
            if (INACTIVE_PLUGINS_FILE.exists()) {
                inactivePluginIds = FileUtils.readLines(INACTIVE_PLUGINS_FILE, true);
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

    private boolean createPluginDirectory() {
        if (!PLUGIN_DIRECTORY.exists()) {
            if (!PLUGIN_DIRECTORY.mkdirs()) {
                LOGGER.warn("Couldn't create the directory for plugins!\n{}", PLUGIN_DIRECTORY.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private void showWarningAlert(String bundleKey, Object... stringParams) {
        Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
                String.format(bundle.getString(bundleKey), stringParams),
                ButtonType.OK);
        alert.initOwner(stageManager.getMainStage());
        alert.show();
    }
}
