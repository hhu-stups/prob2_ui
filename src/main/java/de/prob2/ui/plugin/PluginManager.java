package de.prob2.ui.plugin;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.MainController;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.menu.PluginMenu;
import de.prob2.ui.prob2fx.CurrentTrace;
import edu.umd.cs.findbugs.annotations.NonNull;
import javafx.scene.control.*;
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

    private List<Plugin> registeredPlugins;


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

    public void loadPlugins() {
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
                    loadPlugin(new JarFile(plugin));
                } catch (IOException e) {
                    LOGGER.warn("\n\"{}\" is not a valid ProB-Plugin! \nThe following exception was thrown:", plugin.getName(), e);
                }
            }
        }
    }

    public void addPlugin(File selectedPlugin) {
        File pluginDirectory = new File(PLUGIN_DIRECTORY);
        if (!pluginDirectory.exists()) {
            if (!pluginDirectory.mkdirs()) {
                LOGGER.warn("Couldn't create the directory for plugins!\n{}", pluginDirectory.getAbsolutePath());
            }
            return;
        }
        String pluginName = selectedPlugin.getName();
        File plugin = new File(PLUGIN_DIRECTORY + File.separator + pluginName);
        try {
            Files.copy(selectedPlugin.toPath(),plugin.toPath());
            loadPlugin(new JarFile(plugin));
        } catch (IOException e) {
            LOGGER.warn("Tried to copy the plugin {} \nThis exception was thrown: ", pluginName, e);
        }

    }

    public void stopPlugins() {
        if (registeredPlugins != null && !registeredPlugins.isEmpty()) {
            for (Iterator<Plugin> it = registeredPlugins.iterator(); it.hasNext(); ) {
                Plugin plugin = it.next();
                plugin.stop();
                it.remove();
            }
        }
    }

    private void registerPlugin(@Nonnull final Plugin plugin) {
        if (registeredPlugins == null) {
            registeredPlugins = new ArrayList<>();
        }
        registeredPlugins.add(plugin);
        injector.getInstance(PluginMenu.class).addPluginMenuItem(plugin);

    }

    public void unregisterPlugin(@Nonnull final Plugin plugin) {
        if (registeredPlugins != null) {
            registeredPlugins.remove(plugin);
        }
        injector.getInstance(PluginMenu.class).removePluginMenuItem(plugin);
    }

    public void addTab(@Nonnull final Tab tab) {
        TabPane tabPane = injector.getInstance(MainView.class).getMainTabPane();
        tabPane.getTabs().add(tab);
    }

    public void addMenu(@NonNull final Menu menu) {
        MenuController menuController = injector.getInstance(MenuController.class);
        menuController.getMenus().add(menu);
    }

    public void addMenuItem(@NonNull final MenuEnum menu, @NonNull final MenuItem... items) {
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
        Accordion acc = getAccodion(accordion);
        if (acc != null) {
            acc.getPanes().add(pane);
        }
    }

    //TODO: not working
    public void addPane(AccordionEnum accordion, int position, TitledPane pane) {
        Accordion acc = getAccodion(accordion);
        if (acc != null) acc.getPanes().add(position,pane);
    }

    private Accordion getAccodion(AccordionEnum accordion) {
        MainController mainController = injector.getInstance(MainController.class);
        return (Accordion) mainController.getScene().lookup("#" + accordion.id());
    }

    private void loadPlugin(@Nonnull final JarFile plugin) {
        try {
            URL[]  urls = new URL[]{ new URL("jar:file:" + plugin.getName() +"!/") };
            URLClassLoader classLoader = URLClassLoader.newInstance(urls);

            Enumeration<JarEntry> pluginEntries = plugin.entries();
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
                LOGGER.info("Loading Plugin \"{}\"!", plugin.getName());
                Plugin pl = (Plugin) pluginClass.newInstance();
                pl.start(this);
                registerPlugin(pl);
            } else {
                LOGGER.warn("\"{}\" is not a valid ProB-Plugin!", plugin.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Exception while loading the Plugin \"{}\"", plugin.getName(), e);
        }
    }

}
