package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.output.PrologOutputStage;
import de.prob2.ui.plugin.PluginMenuStage;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.visualisation.fx.VisualisationController;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class AdvancedMenu extends Menu {

	@FXML
	private MenuItem openVisualisationItem;

	@FXML
	private MenuItem stopVisualisationItem;

	@FXML
	private MenuItem detachVisualisationItem;

	private final ProBPluginManager proBPluginManager;
	private final VisualisationController visualisationController;
	private final Injector injector;
	private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedMenu.class);


	@Inject
	public AdvancedMenu(final StageManager stageManager, final ProBPluginManager proBPluginManager,
						   final VisualisationController visualisationController, final Injector injector) {
		this.proBPluginManager = proBPluginManager;
		this.injector = injector;
		stageManager.loadFXML(this, "advancedMenu.fxml");
		this.visualisationController = visualisationController;
		openVisualisationItem.disableProperty().bind(visualisationController.currentMachineProperty().isNull());
		stopVisualisationItem.disableProperty().bind(visualisationController.visualisationProperty().isNull());
		detachVisualisationItem.disableProperty()
				.bind(visualisationController.visualisationProperty().isNull().or(visualisationController.detachProperty()));
	}

	@FXML
	private void handleGroovyConsole() {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}

	@FXML
	private void handlePrologOutput() {
		final Stage prologOutputStage = injector.getInstance(PrologOutputStage.class);
		prologOutputStage.show();
		prologOutputStage.toFront();
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
		pluginMenuStage.toFront();
	}

	@FXML
	private void stopVisualisation() {
		LOGGER.debug("Stop menu-item called.");
		visualisationController.stopVisualisation();
	}

	@FXML
	private void openVisualisation() {
		LOGGER.debug("Open menu-item called.");
		visualisationController.openVisualisation();
	}

	@FXML
	void detachVisualisation() {
		LOGGER.debug("Detach menu-item called.");
		visualisationController.detachVisualisation();
	}

}
