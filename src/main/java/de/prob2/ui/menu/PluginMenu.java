package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.Plugin;
import de.prob2.ui.plugin.PluginManager;
import de.prob2.ui.plugin.PluginMenuStage;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
    private final Injector injector;
    private final StageManager stageManager;


    @Inject
    public PluginMenu(final StageManager stageManager, final PluginManager pluginManager, final Injector injector) {
        this.pluginManager = pluginManager;
        this.stageManager = stageManager;
        this.injector = injector;
        stageManager.loadFXML(this, "pluginMenu.fxml");
    }

    @FXML
    private void addPlugin() {pluginManager.addPlugin();}

    @FXML
    private void reloadPlugins() {
        //TODO: show the user, that we are doing smth
        pluginManager.loadPlugins();
    }

    @FXML
    private void showPluginMenu() {
        PluginMenuStage pluginMenuStage = injector.getInstance(PluginMenuStage.class);
        pluginMenuStage.initModality(Modality.APPLICATION_MODAL);
        pluginMenuStage.initOwner(stageManager.getMainStage());
        pluginMenuStage.show();
    }

}