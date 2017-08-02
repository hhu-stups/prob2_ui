package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.Plugin;
import de.prob2.ui.plugin.PluginManager;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Christoph Heinzen on 27.07.17.
 */
@Singleton
public class PluginMenu extends Menu {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerspectivesMenu.class);

    private final PluginManager pluginManager;
    private final StageManager stageManager;

    @FXML
    private Menu pluginsStopMenu;

    @FXML
    private MenuItem noPluginsMenuItem;

    @Inject
    public PluginMenu(final StageManager stageManager, final PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.stageManager = stageManager;
        stageManager.loadFXML(this, "pluginMenu.fxml");
    }

    @FXML
    private void addPlugin() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Plugin");
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("ProB2 Plugins", "*.jar"));

        final File selectedPlugin = fileChooser.showOpenDialog(stageManager.getMainStage());
        if (selectedPlugin == null) {
            LOGGER.info("No plugin selected!");
            return;
        }

        pluginManager.addPlugin(selectedPlugin);
    }

    public void addPluginMenuItem(Plugin plugin) {
        MenuItem stopEntry = new MenuItem(plugin.getName());
        stopEntry.setOnAction((event) -> {
            plugin.stop();
            pluginManager.unregisterPlugin(plugin);
        });
        stopEntry.setUserData(plugin);
        pluginsStopMenu.getItems().add(stopEntry);
        noPluginsMenuItem.setVisible(false);
    }

    public void removePluginMenuItem(Plugin plugin) {
        MenuItem stopItem = null;
        for (MenuItem item : pluginsStopMenu.getItems()) {
            if(item.getUserData() != null && item.getUserData().equals(plugin)) {
                stopItem = item;
                break;
            }
        }
        if (stopItem != null) {
            pluginsStopMenu.getItems().remove(stopItem);
        }
        if (pluginsStopMenu.getItems().size() == 1) {
            noPluginsMenuItem.setVisible(true);
        }
    }

}