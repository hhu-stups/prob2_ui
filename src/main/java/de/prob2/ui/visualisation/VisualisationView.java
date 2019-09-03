package de.prob2.ui.visualisation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

@FXMLInjected
@Singleton
public class VisualisationView extends AnchorPane {
	@FXML
	private StackPane probLogoStackPane;
	@FXML
	private ScrollPane visualisationScrollPane;
	@FXML
	private StateVisualisationView currentStateVisualisation;
	@FXML
	private StateVisualisationView previousStateVisualisation;
	@FXML
	private VBox previousStateVBox;
	@FXML
	private Label placeholderLabel;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final CurrentProject currentProject, final StageManager stageManager, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoStackPane.visibleProperty().not());
		probLogoStackPane.visibleProperty().bind(currentStateVisualisation.visualisationPossibleProperty().not());
		previousStateVBox.managedProperty().bind(previousStateVisualisation.visualisationPossibleProperty());
		previousStateVBox.visibleProperty().bind(previousStateVBox.managedProperty());

		currentTrace.addListener((observable, from, to) -> {
			if(to == null) {
				placeholderLabel.setText(bundle.getString("common.noModelLoaded"));
				currentStateVisualisation.visualiseState(null);
				previousStateVisualisation.visualiseState(null);
			} else {
				if (!currentTrace.getCurrentState().isInitialised()) {
					placeholderLabel.setText(bundle.getString("common.notInitialised"));
				} else {
					placeholderLabel.setText(bundle.getString("visualisation.view.placeholder.noAnimationFunction"));
				}
				
				currentStateVisualisation.visualiseState(to.getCurrentState());
				if (to.canGoBack()) {
					previousStateVisualisation.visualiseState(to.getPreviousState());
				}
			}
		});

		this.currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			this.currentStateVisualisation.getMachineImages().clear();
			this.previousStateVisualisation.getMachineImages().clear();
			if (to != null) {
				final GetImagesForMachineCommand cmd = new GetImagesForMachineCommand();
				to.execute(cmd);
				final Map<Integer, Image> machineImages = this.loadMachineImages(cmd.getImages());
				this.currentStateVisualisation.getMachineImages().putAll(machineImages);
				this.previousStateVisualisation.getMachineImages().putAll(machineImages);
			}
		});
	}

	private Map<Integer, Image> loadMachineImages(final Map<Integer, String> imageNames) {
		final Path projectDirectory = currentProject.get().getLocation();
		final Path machineFile = currentProject.getCurrentMachine().getLocation();
		final Path machineDirectory = projectDirectory.resolve(machineFile).getParent();
		final Path proBDirectory = Paths.get(Main.getProBDirectory());
		final List<Path> imageDirectories = Arrays.asList(machineDirectory, projectDirectory, proBDirectory);

		final Map<Integer, Image> machineImages = new HashMap<>();
		final List<String> notFoundImages = new ArrayList<>();
		imageNames.forEach((number, name) -> {
			boolean found = false;
			for (final Path dir : imageDirectories) {
				final Path imagePath = dir.resolve(name);
				if (imagePath.toFile().exists()) {
					machineImages.put(number, new Image(imagePath.toUri().toString()));
					found = true;
					break;
				}
			}
			if (!found) {
				notFoundImages.add(name);
			}
		});

		if (!notFoundImages.isEmpty()) {
			this.stageManager.makeAlert(
				Alert.AlertType.WARNING,
				"visualisation.stateVisualisationView.alerts.imagesNotFound.header",
				"visualisation.stateVisualisationView.alerts.imagesNotFound.content",
				String.join("\n", notFoundImages),
				machineDirectory,
				projectDirectory,
				proBDirectory
			).show();
		}

		return machineImages;
	}
}
