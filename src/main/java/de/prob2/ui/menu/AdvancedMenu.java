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
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.visualisation.fx.VisualisationController;

import de.prob2.ui.vomanager.VOManagerStage;
import javafx.beans.binding.Bindings;
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

	@FXML
	private MenuItem openVOManagerItem;

	private final ProBPluginManager proBPluginManager;
	private final VisualisationController visualisationController;
	private final Injector injector;
	private final CurrentProject currentProject;
	private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedMenu.class);


	@Inject
	public AdvancedMenu(final StageManager stageManager, final ProBPluginManager proBPluginManager,
						final VisualisationController visualisationController, final Injector injector,
						final CurrentProject currentProject) {
		this.proBPluginManager = proBPluginManager;
		this.injector = injector;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "advancedMenu.fxml");
		this.visualisationController = visualisationController;
		openVisualisationItem.disableProperty().bind(visualisationController.currentMachineProperty().isNull());
		stopVisualisationItem.disableProperty().bind(visualisationController.visualisationProperty().isNull());
		detachVisualisationItem.disableProperty()
				.bind(visualisationController.visualisationProperty().isNull().or(visualisationController.detachProperty()));
	}

	@FXML
	private void initialize() {
		openVOManagerItem.disableProperty().bind(Bindings.createBooleanBinding(() -> currentProject.get() == null, currentProject));
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

	@FXML
	private void openSimulator() {
		SimulatorStage simulatorStage = injector.getInstance(SimulatorStage.class);
		simulatorStage.show();
		simulatorStage.toFront();
	}

	@FXML
	private void openVOManager() {
		VOManagerStage voManagerStage = injector.getInstance(VOManagerStage.class);
		voManagerStage.show();
		voManagerStage.toFront();
	}

}
