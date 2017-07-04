package de.prob2.ui.visualisation;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.commands.ExecuteRightClickCommand;
import de.prob2.ui.commands.GetImagesForMachineCommand;
import de.prob2.ui.commands.GetImagesForStateCommand;
import de.prob2.ui.commands.GetRightClickOptionsForStateVisualizationCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class StateVisualisationView extends AnchorPane {
	@FXML
	private GridPane visualisationGridPane;

	private final CurrentProject currentProject;
	private BooleanProperty visualisationPossible = new SimpleBooleanProperty(false);

	@Inject
	public StateVisualisationView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "state_visualisation_view.fxml");
	}

	public void visualiseState(State state) throws FileNotFoundException {
		visualisationGridPane.getChildren().clear();
		visualisationPossible.set(false);
		if(state == null || !state.isInitialised() || currentProject.getCurrentRunconfiguration() == null) {
			return;
		}
		StateSpace stateSpace = state.getStateSpace();
		GetImagesForMachineCommand getImagesForMachineCommand = new GetImagesForMachineCommand();
		stateSpace.execute(getImagesForMachineCommand);
		Map<Integer, String> images = getImagesForMachineCommand.getImages();
		if (!images.isEmpty()) {
			showImages(state, images);
			visualisationPossible.set(true);
		}

	}
	
	public BooleanProperty visualisationPossibleProperty() {
		return visualisationPossible;
	}

	private void showImages(State state, Map<Integer, String> images) throws FileNotFoundException {
		GetImagesForStateCommand getImagesForStateCommand = new GetImagesForStateCommand(state.getId());
		state.getStateSpace().execute(getImagesForStateCommand);
		int[][] imageMatrix = getImagesForStateCommand.getMatrix();
		int rowNr = getImagesForStateCommand.getRows();
		int columnNr = getImagesForStateCommand.getColumns();

		for (int r = 0; r < rowNr; r++) {
			for (int c = 0; c < columnNr; c++) {
				String imageURL = images.get(imageMatrix[r][c]);
				Image image = getImage(imageURL);
				ImageView imageView = new ImageView(image);
				ContextMenu contextMenu = getContextMenu(state, r, c);
				imageView.setOnContextMenuRequested(e -> contextMenu.show(imageView, e.getScreenX(), e.getScreenY()));
				visualisationGridPane.add(imageView, c, r);
			}
		}
	}

	private ContextMenu getContextMenu(State state, int row, int column) {
		StateSpace stateSpace = state.getStateSpace();
		ContextMenu contextMenu = new ContextMenu();
		GetRightClickOptionsForStateVisualizationCommand getOptionsCommand = new GetRightClickOptionsForStateVisualizationCommand(
				state.getId(), row, column);
		stateSpace.execute(getOptionsCommand);
		List<String> options = getOptionsCommand.getOptions();
		for (String opt : options) {
			final MenuItem item = new MenuItem(opt);
			item.setOnAction(e -> {
				ExecuteRightClickCommand executeCommand = new ExecuteRightClickCommand(state.getId(), row, column, opt);
				stateSpace.execute(executeCommand);
			});
			contextMenu.getItems().add(item);
		}
		if (options.isEmpty()) {
			final MenuItem item = new MenuItem("no right click options defined");
			item.setDisable(true);
			contextMenu.getItems().add(item);
		}
		return contextMenu;
	}

	private Image getImage(String imageURL) throws FileNotFoundException {
		final Path projectFolder = Paths.get(currentProject.get().getLocation().getPath());
		// look in machine folder
		final Path machineFile = currentProject.getCurrentRunconfiguration().getMachine().getPath();
		final Path machineFolder = projectFolder.resolve(machineFile).getParent();
		File imagePath = machineFolder.resolve(imageURL).toFile();
		final File imageInMachineFolder = imagePath;
		// look in project folder
		if (!imageInMachineFolder.exists()) {
			imagePath = projectFolder.resolve(imageURL).toFile();
			final File imageInProjectFolder = imagePath;
			if (!imageInProjectFolder.exists()) {
				// look in ProB folder
				imagePath = Paths.get(Main.getProBDirectory()).resolve(imageURL).toFile();
				final File imageInProbFolder = imagePath;
				if (!imageInProbFolder.exists()) {
					throw new FileNotFoundException("Image " + imagePath.getName() + " not found in machine folder ("
							+ imageInMachineFolder.getParent() + ") and project folder ("
							+ imageInProjectFolder.getParent() + ") and ProB folder (" + imageInProbFolder.getParent()
							+ ")");
				}
			}
		}
		return new Image(imagePath.toURI().toString());
	}
}
