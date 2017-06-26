package de.prob2.ui.visualisation;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;

import de.prob2.ui.commands.GetImagesForMachineCommand;
import de.prob2.ui.commands.GetImagesForStateCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VisualisationView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationView.class);
	
	@FXML
	private StackPane probLogoStackPane;
	@FXML
	private ScrollPane visualisationScrollPane;
	@FXML
	private GridPane visualisationGridPane;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoStackPane.visibleProperty().not());
		
		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			visualisationGridPane.getChildren().clear();

			if (to != null && to.isInitialised()) {
				StateSpace stateSpace = to.getStateSpace();

				GetImagesForMachineCommand getImagesForMachineCommand = new GetImagesForMachineCommand();
				stateSpace.execute(getImagesForMachineCommand);
				Map<Integer, String> images = getImagesForMachineCommand.getImages();
				if (!images.isEmpty()) {
					try {
						showImages(to, stateSpace, images);
						probLogoStackPane.setVisible(false);
					} catch (FileNotFoundException e) {
						LOGGER.warn("Failed to open images for visualisation", e);
						Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, e.getMessage());
						alert.setHeaderText("Visualisation not possible");
						alert.showAndWait();
					}
				} else {
					probLogoStackPane.setVisible(true);
				}
			} else {
				probLogoStackPane.setVisible(true);
			}
		});

	}

	private void showImages(State state, StateSpace stateSpace, Map<Integer, String> images) throws FileNotFoundException {
		GetImagesForStateCommand getImagesForStateCommand = new GetImagesForStateCommand(state.getId());
		stateSpace.execute(getImagesForStateCommand);
		int[][] imageMatrix = getImagesForStateCommand.getMatrix();
		int rowNr = getImagesForStateCommand.getRows();
		int columnNr = getImagesForStateCommand.getColumns();

		for (int r = 0; r < rowNr; r++) {
			for (int c = 0; c < columnNr; c++) {
				String imageURL = images.get(imageMatrix[r][c]);
				Image image = getImage(imageURL);
				ImageView imageView = new ImageView(image);
				visualisationGridPane.add(imageView, c, r);
			}
		}
	}

	private Image getImage(String imageURL) throws FileNotFoundException {
		final Path projectFolder = Paths.get(currentProject.get().getLocation().getPath());
		//look in machine folder
		final Path machineFile = currentProject.getCurrentRunconfiguration().getMachine().getPath();
		final Path machineFolder = projectFolder.resolve(machineFile).getParent();
		File imagePath = machineFolder.resolve(imageURL).toFile();
		final File imageInMachineFolder = imagePath;
		//look in project folder
		if (!imageInMachineFolder.exists()) {
			imagePath = projectFolder.resolve(imageURL).toFile();
			final File imageInProjectFolder = imagePath;
			if(!imageInProjectFolder.exists()) {
				//look in ProB folder
				imagePath = Paths.get(Main.getProBDirectory()).resolve(imageURL).toFile();
				final File imageInProbFolder = imagePath;
				if(!imageInProbFolder.exists()) {
					throw new FileNotFoundException("Image " + imagePath.getName() + " not found in machine folder (" + imageInMachineFolder.getParent() 
							+ ") and project folder (" + imageInProjectFolder.getParent() + ") and ProB folder (" + imageInProbFolder.getParent() + ")");
				}
			}
		}
		return new Image(imagePath.toURI().toString());
	}
}
