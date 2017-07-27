package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.PluginManager;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Christoph Heinzen on 27.07.17.
 */
public class PluginMenu extends Menu {
    private static final Logger logger = LoggerFactory.getLogger(PerspectivesMenu.class);

    private final Injector injector;
    private final StageManager stageManager;

    @FXML
    private Menu pluginsStopMenu;

    @Inject
    public PluginMenu(final StageManager stageManager, final Injector injector) {
        this.injector = injector;
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
            return;
        }

        PluginManager pluginManager = injector.getInstance(PluginManager.class);
        pluginManager.addPlugin(selectedPlugin);
    }

}
