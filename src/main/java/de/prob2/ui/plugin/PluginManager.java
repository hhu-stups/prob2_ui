package de.prob2.ui.plugin;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.MainController;
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

    public enum MenuEnum {
        FILE_MENU("fileMenu"),
        RECENT_PROJECTS_MENU("recentProjectsMenu"),
        EDIT_MENU("editMenu"),
        FORMULA_MENU("formulaMenu"),
        CONSOLES_MENU("consolesMenu"),
        PERSPECTIVES_MENU("perspectivesMenu"),
        PRESET_PERSPECTIVES_MENU("presetPerspectivesMenu"),
        VIEW_MENU("viewMenu"),
        PLUGIN_MENU("pluginMenu"),
        PLUGINS_STOP_MENU("pluginsStopMenu"),
        WINDOW_MENU("windowMenu"),
        HELP_MENU("helpMenu");

        private final String id;

        MenuEnum(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }

    public enum AccordionEnum {
        RIGHT_ACCORDION("rightAccordion"),
        RIGHT_ACCORDION_1("rightAccordion1"),
        RIGHT_ACCORDION_2("rightAccordion2"),
        LEFT_ACCORDION("leftAccordion"),
        LEFT_ACCORDION_1("leftAccordion1"),
        LEFT_ACCORDION_2("leftAccordion2"),
        TOP_ACCORDION("topAccordion"),
        TOP_ACCORDION_1("topAccordion1"),
        TOP_ACCORDION_2("topAccordion2"),
        TOP_ACCORDION_3("topAccordion3"),
        BOTTOM_ACCORDION("bottomAccordion"),
        BOTTOM_ACCORDION_1("bottomAccordion1"),
        BOTTOM_ACCORDION_2("bottomAccordion2"),
        BOTTOM_ACCORDION_3("bottomAccordion3");

        private final String id;

        AccordionEnum(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
    private static final String PLUGIN_DIRECTORY = System.getProperty("user.home")
            + File.separator + ".prob"
            + File.separator + "prob2"
            + File.separator + "prob2ui"
            + File.separator + "plugins";

    private final Injector injector;
    private final CurrentTrace currentTrace;

    private List<Plugin> registeredPlugins;


    //TODO: test what happens when two plugins use the same library in different versions and what happens when two plugins have the same package structure

    @Inject
    public PluginManager(Injector injector, CurrentTrace currentTrace){
        this.injector = injector;
        this.currentTrace = currentTrace;
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
        MainController mainController = injector.getInstance(MainController.class);
        TabPane tabPane = (TabPane) mainController.getScene().lookup("#mainTabPane");
        tabPane.getTabs().add(tab);
    }

    public void addMenu(@NonNull final Menu menu) {
        MenuController menuController = injector.getInstance(MenuController.class);
        menuController.getMenus().add(menu);
    }

    public void addMenuItem(@NonNull final MenuEnum menu, @NonNull final MenuItem... items) {
        Menu menuToAddItems = searchMenu(menu);
        if (menuToAddItems != null) {
            menuToAddItems.getItems().addAll(items);
        } else {
            LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
        }
    }

    public void addMenuItem(@Nonnull final MenuEnum menu,
                            final int position,
                            @Nonnull final MenuItem... items) {
        Menu menuToAddItems = searchMenu(menu);
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

    private Menu searchMenu(@Nonnull final MenuEnum searchedMenu) {
        MenuController menuController = injector.getInstance(MenuController.class);
        for (Menu menu : menuController.getMenus()) {
            if (searchedMenu.id().equals(menu.getId())) {
                return menu;
            }
            Menu subMenu = searchMenuInSubMenus(menu, searchedMenu);
            if (subMenu != null) {
                return subMenu;
            }
        }
        return null;
    }

    private Menu searchMenuInSubMenus(@Nonnull final Menu menuToSearchIn,
                                      @Nonnull final MenuEnum searchedMenu){
        for (MenuItem item : menuToSearchIn.getItems()) {
            if (item instanceof Menu) {
                Menu subMenu = (Menu) item;
                if (searchedMenu.id().equals(subMenu.getId())) {
                    return subMenu;
                }
                Menu ret = searchMenuInSubMenus(subMenu, searchedMenu);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
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
