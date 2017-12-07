package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.fx.VisualisationController;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @since 21.09.17
 */
@Singleton
public class VisualisationMenu extends Menu{

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationMenu.class);

	@FXML
	private MenuItem openVisualisationItem;

	@FXML
	private MenuItem stopVisualisationItem;

	@FXML
	private MenuItem detachVisualisationItem;

	private final VisualisationController visualisationController;
	
	private final DotView dotView;

	@Inject
	public VisualisationMenu(final StageManager stageManager, final VisualisationController visualisationController, final DotView dotView) {
		this.visualisationController = visualisationController;
		this.dotView = dotView;
		stageManager.loadFXML(this, "visualisationMenu.fxml");
	}

	@FXML
	public void initialize() {
		LOGGER.debug("Initializing the visualization-menu!");
		openVisualisationItem.disableProperty().bind(visualisationController.currentMachineProperty().isNull());
		stopVisualisationItem.disableProperty().bind(visualisationController.visualisationProperty().isNull());
		detachVisualisationItem.disableProperty()
				.bind(visualisationController.visualisationProperty().isNull().or(visualisationController.detachProperty()));
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
	private void showCurrentState() {
		dotView.show();
	}
	

}
