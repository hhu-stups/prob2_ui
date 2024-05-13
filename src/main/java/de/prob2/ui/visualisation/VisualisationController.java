package de.prob2.ui.visualisation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.annotations.Home;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

@Singleton
public final class VisualisationController {
	@FXML
	private VBox probLogoView;
	@FXML
	private ScrollPane visualisationScrollPane;
	@FXML
	private Node currentStateVisualisation;
	@FXML
	private StateVisualisationController currentStateVisualisationController;
	@FXML
	private Node previousStateVisualisation;
	@FXML
	private StateVisualisationController previousStateVisualisationController;
	@FXML
	private VBox previousStateVBox;
	@FXML
	private Label placeholderLabel;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final I18n i18n;
	private final Path proBHomePath;
	private final BackgroundUpdater updater;

	@Inject
	private VisualisationController(final CurrentTrace currentTrace, final CurrentProject currentProject, final StageManager stageManager, final I18n i18n, final @Home Path proBHomePath, final StopActions stopActions, final StatusBar statusBar) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.proBHomePath = proBHomePath;
		this.updater = new BackgroundUpdater("State visualisation updater");
		stopActions.add(this.updater::shutdownNow);
		statusBar.addUpdatingExpression(this.updater.runningProperty());
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoView.visibleProperty().not());
		probLogoView.visibleProperty().bind(currentStateVisualisationController.visualisationPossibleProperty().not());
		previousStateVBox.managedProperty().bind(previousStateVisualisationController.visualisationPossibleProperty());
		previousStateVBox.visibleProperty().bind(previousStateVBox.managedProperty());

		this.updater.runningProperty().addListener((o, from, to) -> Platform.runLater(() -> {
			this.currentStateVisualisation.setDisable(to);
			this.previousStateVisualisation.setDisable(to);
		}));

		currentTrace.addListener((o, from, to) -> {
			if (to == null) {
				placeholderLabel.setText(i18n.translate("common.noModelLoaded"));
			} else if (!currentTrace.getCurrentState().isInitialised()) {
				placeholderLabel.setText(i18n.translate("common.notInitialised"));
			} else {
				placeholderLabel.setText(i18n.translate("visualisation.view.placeholder.noAnimationFunction"));
			}

			if (from == null || to == null || !from.getStateSpace().equals(to.getStateSpace())) {
				this.currentStateVisualisationController.getMachineImages().clear();
				this.previousStateVisualisationController.getMachineImages().clear();
			}

			updater.execute(() -> {
				if (to != null && (from == null || !from.getStateSpace().equals(to.getStateSpace()))) {
					GetImagesForMachineCommand cmd = new GetImagesForMachineCommand();
					to.getStateSpace().execute(cmd);
					Map<Integer, Image> machineImages = this.loadMachineImages(cmd.getImages());
					Platform.runLater(() -> {
						this.currentStateVisualisationController.getMachineImages().putAll(machineImages);
						this.previousStateVisualisationController.getMachineImages().putAll(machineImages);
					});
				}
				currentStateVisualisationController.visualiseState(to);
				previousStateVisualisationController.visualiseState(to != null && to.canGoBack() ? to.back() : null);
			});
		});
	}

	private Map<Integer, Image> loadMachineImages(final Map<Integer, String> imageNames) {
		final Path projectDirectory = currentProject.get().getLocation();
		//getParent is required to access the directory of the machine
		final Path machineDirectory = currentProject.get().getAbsoluteMachinePath(currentProject.getCurrentMachine()).getParent();
		final List<Path> imageDirectories = Arrays.asList(machineDirectory, projectDirectory, proBHomePath);

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
			Platform.runLater(() -> {
				final Alert alert = this.stageManager.makeAlert(
					Alert.AlertType.WARNING,
					"visualisation.stateVisualisationView.alerts.imagesNotFound.header",
					"visualisation.stateVisualisationView.alerts.imagesNotFound.content",
					String.join("\n", notFoundImages),
					machineDirectory,
					projectDirectory,
					proBHomePath
				);
				alert.initOwner(probLogoView.getScene().getWindow());
				alert.show();
			});
		}

		return machineImages;
	}
}
