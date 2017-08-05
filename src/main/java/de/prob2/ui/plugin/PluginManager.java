package de.prob2.ui.plugin;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Singleton
public class PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
    private static final String PLUGIN_DIRECTORY = System.getProperty("user.home")
            + File.separator + ".prob"
            + File.separator + "prob2"
            + File.separator + "prob2ui"
            + File.separator + "plugins";

    private final Injector injector;
    private final CurrentTrace currentTrace;
    private final StageManager stageManager;

    private ObservableMap<Plugin, Boolean> activePlugins;
    private Map<Plugin, String> pluginFiles;
    //private List<Plugin> registeredPlugins;


    //TODO: test what happens when two plugins use the same library in different versions and what happens when two plugins have the same package structure

    @Inject
    public PluginManager(Injector injector, CurrentTrace currentTrace, StageManager stageManager){
        this.injector = injector;
        this.currentTrace = currentTrace;
        this.stageManager = stageManager;
        this.currentTrace.addListener((observable, oldValue, newValue) -> {
            //TODO: implement
        });
    }

    public ObservableMap<Plugin, Boolean> getActivePlugins(){
        if (activePlugins == null) {
            activePlugins = FXCollections.observableHashMap();
        }
        return activePlugins;
    }

    public void loadPlugins() {
        stopPlugins(); //stop all plugins and remove them from the maps
        File pluginDirectory = new File(PLUGIN_DIRECTORY);
        if (!pluginDirectory.exists()) {
            if (!pluginDirectory.mkdirs()) {
                LOGGER.warn("Couldn't create the directory for plugins!\n{}", pluginDirectory.getAbsolutePath());
            }
            return;
        }
        File[] plugins = pluginDirectory.listFiles();
        if (plugins != null && plugins.length != 0) {
            for (File plugin : plugins) {
                try {
                    //TODO: check if the plugin should be active or not
                    loadPlugin(new JarFile(plugin), true);
                } catch (IOException e) {
                    LOGGER.warn("\n\"{}\" is not a valid ProB-Plugin! \nThe following exception was thrown:", plugin.getName(), e);
                }
            }
        }
    }

    public void stopPlugins() {
        if (activePlugins != null && !activePlugins.isEmpty()) {
            activePlugins.forEach((plugin, active) -> {
                if (active)
                    plugin.stop();
            });
            activePlugins.clear();
            pluginFiles.clear();
        }
    }

    public void addPlugin() {
        addPlugin(stageManager.getMainStage());
    }

    public void addPlugin(@Nonnull Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Plugin");
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("ProB2 Plugins", "*.jar"));

        final File selectedPlugin = fileChooser.showOpenDialog(stage);
        if (selectedPlugin != null) {
            File pluginDirectory = new File(PLUGIN_DIRECTORY);
            if (!pluginDirectory.exists()) {
                if (!pluginDirectory.mkdirs()) {
                    LOGGER.warn("Couldn't create the directory for plugins!\n{}", pluginDirectory.getAbsolutePath());
                }
                return;
            }
            File plugin = new File(PLUGIN_DIRECTORY + File.separator + selectedPlugin.getName());
            try {
                Files.copy(selectedPlugin.toPath(), plugin.toPath());
                loadPlugin(new JarFile(plugin), true);
            } catch (IOException e) {
                LOGGER.warn("Tried to copy the plugin {} \nThis exception was thrown: ", selectedPlugin.getName(), e);
            }
        }
    }

    public void removePlugin(Plugin plugin) {
        if (activePlugins.get(plugin)) {
            plugin.stop();
        }
        File pluginFile = new File(PLUGIN_DIRECTORY + File.separator + pluginFiles.get(plugin));
        try {
            Files.delete(pluginFile.toPath());
        } catch (IOException e) {
            stageManager.makeAlert(Alert.AlertType.WARNING,
                    String.format("Could not delete the Jar-File of the plugin %s!" +
                            "\nPlease try to delete it manually!", plugin.getName()),
                    ButtonType.OK).show();
        }
    }

    private void registerPlugin(@Nonnull final Plugin plugin, boolean active, String fileName) {
        if (activePlugins == null) {
            activePlugins = FXCollections.observableHashMap();
        }
        activePlugins.put(plugin, active);
        if (pluginFiles == null) {
            pluginFiles = new HashMap<>();
        }
        pluginFiles.put(plugin, fileName);
    }

    private void loadPlugin(@Nonnull final JarFile pluginJar, boolean active) {
        try {
            URL[]  urls = new URL[]{ new URL("jar:file:" + pluginJar.getName() +"!/") };
            URLClassLoader classLoader = URLClassLoader.newInstance(urls);

            Enumeration<JarEntry> pluginEntries = pluginJar.entries();
            boolean loadPlugin = false;
            Class pluginClass = null;
            while (pluginEntries.hasMoreElements()) {
                JarEntry jarEntry = pluginEntries.nextElement();
                if(jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")){
                    continue;
                }
                String className = jarEntry.getName().substring(0,jarEntry.getName().length()-6);
                className = className.replace('/', '.');
                pluginClass = classLoader.loadClass(className);
                if (pluginClass != null &&
                        pluginClass.getSuperclass() != null &&
                        pluginClass.getSuperclass().equals(Plugin.class)) {
                    loadPlugin = true;
                    break;
                }
            }
            if (loadPlugin) {
                LOGGER.info("Loading Plugin \"{}\"!", pluginJar.getName());
                Plugin plugin = (Plugin) pluginClass.newInstance();
                //TODO: check if the Plugin is already started, maybe in an other version and that the version of the plugin is sufficient for the used version of ProB
                plugin.start(this);
                registerPlugin(plugin, active, pluginJar.getName());
            } else {
                LOGGER.warn("\"{}\" is not a valid ProB-Plugin!", pluginJar.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Exception while loading the Plugin \"{}\"", pluginJar.getName(), e);
        }
    }


    public void addTab(@Nonnull final Tab tab) {
        TabPane tabPane = injector.getInstance(MainView.class).getMainTabPane();
        tabPane.getTabs().add(tab);
    }

    public void addMenu(@Nonnull final Menu menu) {
        MenuController menuController = injector.getInstance(MenuController.class);
        menuController.getMenus().add(menu);
    }

    public void addMenuItem(@Nonnull final MenuEnum menu, @Nonnull final MenuItem... items) {
        Menu menuToAddItems = menu.searchMenu(injector);
        if (menuToAddItems != null) {
            menuToAddItems.getItems().addAll(items);
        } else {
            LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
        }
    }

    public void addMenuItem(@Nonnull final MenuEnum menu,
                            final int position,
                            @Nonnull final MenuItem... items) {
        Menu menuToAddItems = menu.searchMenu(injector);
        if (menuToAddItems != null) {
            menuToAddItems.getItems().addAll(position, Arrays.asList(items));
        } else {
            LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
        }
    }

    //TODO: not working
    public void addPane(AccordionEnum accordion, TitledPane pane) {
        Accordion acc = accordion.getAccordion(injector);
        //TODO: react when the Accordion doesn't exist
        if (acc != null) {
            acc.getPanes().add(pane);
        }
    }

    //TODO: not working
    public void addPane(AccordionEnum accordion, int position, TitledPane pane) {
        Accordion acc = accordion.getAccordion(injector);
        //TODO: react when the Accordion doesn't exist
        if (acc != null) acc.getPanes().add(position,pane);
    }

}
