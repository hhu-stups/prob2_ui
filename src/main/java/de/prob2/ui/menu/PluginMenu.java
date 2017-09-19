package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.plugin.PluginMenuStage;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Christoph Heinzen on 27.07.17.
 */
@Singleton
public class PluginMenu extends Menu {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerspectivesMenu.class);

    private final ProBPluginManager proBPluginManager;
    private final Injector injector;
    private final StageManager stageManager;


    @Inject
    public PluginMenu(final StageManager stageManager, final ProBPluginManager proBPluginManager, final Injector injector) {
        this.proBPluginManager = proBPluginManager;
        this.stageManager = stageManager;
        this.injector = injector;
        stageManager.loadFXML(this, "pluginMenu.fxml");
    }

    @FXML
    private void addPlugin() {
        proBPluginManager.addPlugin();}

    @FXML
    private void reloadPlugins() {
        proBPluginManager.reloadPlugins();
    }

    @FXML
    private void showPluginMenu() {
        PluginMenuStage pluginMenuStage = injector.getInstance(PluginMenuStage.class);
        pluginMenuStage.show();
    }

}