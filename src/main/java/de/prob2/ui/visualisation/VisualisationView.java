package de.prob2.ui.visualisation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.statespace.State;
import de.prob2.ui.internal.BackgroundUpdater;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
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
	private final I18n i18n;

	private final BackgroundUpdater updater;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final CurrentProject currentProject, final StageManager stageManager, final I18n i18n, final StopActions stopActions, final StatusBar statusBar) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.updater = new BackgroundUpdater("VisualisationView Updater");
		stopActions.add(this.updater::shutdownNow);
		statusBar.addUpdatingExpression(this.updater.runningProperty());
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoStackPane.visibleProperty().not());
		probLogoStackPane.visibleProperty().bind(currentStateVisualisation.visualisationPossibleProperty().not());
		previousStateVBox.managedProperty().bind(previousStateVisualisation.visualisationPossibleProperty());
		previousStateVBox.visibleProperty().bind(previousStateVBox.managedProperty());

		this.updater.runningProperty().addListener((o, from, to) -> {
			Platform.runLater(() -> {
				this.currentStateVisualisation.setDisable(to);
				this.previousStateVisualisation.setDisable(to);
			});
		});

		currentTrace.addListener((o, from, to) -> {
			if (to == null) {
				placeholderLabel.setText(i18n.translate("common.noModelLoaded"));
			} else if (!currentTrace.getCurrentState().isInitialised()) {
				placeholderLabel.setText(i18n.translate("common.notInitialised"));
			} else {
				placeholderLabel.setText(i18n.translate("visualisation.view.placeholder.noAnimationFunction"));
			}
			
			updater.execute(() -> {
				visualiseState(currentStateVisualisation, to != null ? to.getCurrentState() : null);
				visualiseState(previousStateVisualisation, to != null && to.canGoBack() ? to.getPreviousState() : null);
			});
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
		//getParent is required to access the directory of the machine
		final Path machineDirectory = currentProject.get().getAbsoluteMachinePath(currentProject.getCurrentMachine()).getParent();
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
			final Alert alert = this.stageManager.makeAlert(
				Alert.AlertType.WARNING,
				"visualisation.stateVisualisationView.alerts.imagesNotFound.header",
				"visualisation.stateVisualisationView.alerts.imagesNotFound.content",
				String.join("\n", notFoundImages),
				machineDirectory,
				projectDirectory,
				proBDirectory
			);
			alert.initOwner(this.getScene().getWindow());
			alert.show();
		}

		return machineImages;
	}

	private static void visualiseState(final StateVisualisationView view, final State state) {
		final List<List<AnimationMatrixEntry>> matrix;
		if (state == null) {
			matrix = Collections.emptyList();
		} else {
			final GetAnimationMatrixForStateCommand cmd = new GetAnimationMatrixForStateCommand(state);
			state.getStateSpace().execute(cmd);
			matrix = cmd.getMatrix();
		}
		
		Platform.runLater(() -> {
			view.getChildren().clear();
			final boolean it = !matrix.isEmpty();
			view.visualisationPossibleProperty().set(it);
			if (it) {
				view.showMatrix(state, matrix);
			}
		});
	}
}
