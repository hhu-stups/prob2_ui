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

import de.prob.Main;
import de.prob.animator.command.ExecuteRightClickCommand;
import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.command.GetRightClickOptionsForStateVisualizationCommand;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

@FXMLInjected
public class StateVisualisationView extends AnchorPane {
	@FXML
	private GridPane visualisationGridPane;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private BooleanProperty visualisationPossible = new SimpleBooleanProperty(false);
	private final Map<Integer, Image> machineImages;

	@Inject
	public StateVisualisationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.machineImages = new HashMap<>();
		this.currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			this.machineImages.clear();
			if (to != null) {
				final GetImagesForMachineCommand cmd = new GetImagesForMachineCommand();
				to.execute(cmd);
				this.loadMachineImages(cmd.getImages());
			}
		});
		stageManager.loadFXML(this, "state_visualisation_view.fxml");
	}

	public void visualiseState(State state) {
		visualisationGridPane.getChildren().clear();
		visualisationPossible.set(false);
		if (state == null || !state.isInitialised() || currentProject.getCurrentMachine() == null) {
			return;
		}
		showMatrix(state);
	}

	public BooleanProperty visualisationPossibleProperty() {
		return visualisationPossible;
	}

	private void showMatrix(State state) {
		final GetAnimationMatrixForStateCommand cmd = new GetAnimationMatrixForStateCommand(state);
		state.getStateSpace().execute(cmd);
		final List<List<AnimationMatrixEntry>> matrix = cmd.getMatrix();
		if (matrix.isEmpty()) {
			visualisationPossible.set(false);
			return;
		} else {
			visualisationPossible.set(true);
		}

		for (int r = 0; r < matrix.size(); r++) {
			final List<AnimationMatrixEntry> row = matrix.get(r);
			for (int c = 0; c < row.size(); c++) {
				final AnimationMatrixEntry entry = row.get(c);
				final Node view;
				if (entry == null) {
					view = null;
				} else if (entry instanceof AnimationMatrixEntry.Image) {
					final int number = ((AnimationMatrixEntry.Image)entry).getImageNumber(); 
					if (this.machineImages.containsKey(number)) {
						view = new ImageView(this.machineImages.get(number));
					} else {
						view = null;
					}
				} else if (entry instanceof AnimationMatrixEntry.Text) {
					view = new Label(((AnimationMatrixEntry.Text)entry).getText());
				} else {
					throw new AssertionError("Unhandled animation matrix entry type: " + entry.getClass());
				}

				if (view != null) {
					view.setOnContextMenuRequested(e -> getContextMenu(state, entry)
						.show(view, e.getScreenX(), e.getScreenY()));
					visualisationGridPane.add(view, c, r);
				}
			}
		}
	}

	private ContextMenu getContextMenu(State state, AnimationMatrixEntry entry) {
		ContextMenu contextMenu = new ContextMenu();
		StateSpace stateSpace = state.getStateSpace();
		GetRightClickOptionsForStateVisualizationCommand getOptionsCommand = new GetRightClickOptionsForStateVisualizationCommand(
				state.getId(), entry.getRow(), entry.getColumn());
		stateSpace.execute(getOptionsCommand);
		List<String> options = getOptionsCommand.getOptions();
		for (String opt : options) {
			final MenuItem item = new MenuItem(opt);
			Trace trace = getTraceToState(currentTrace.get(), state);
			if (trace == null) {
				item.setDisable(true);
			}
			item.setOnAction(e -> {
				ExecuteRightClickCommand executeCommand = new ExecuteRightClickCommand(state.getId(), entry.getRow(), entry.getColumn(), opt);
				stateSpace.execute(executeCommand);
				String transitionId = executeCommand.getTransitionID();
				currentTrace.set(trace.add(transitionId));
			});
			contextMenu.getItems().add(item);
		}
		if (options.isEmpty()) {
			final MenuItem item = new MenuItem(bundle.getString("visualisation.stateVisualisationView.contextMenu.noRightClickOptions"));
			item.setDisable(true);
			contextMenu.getItems().add(item);
		}
		return contextMenu;
	}

	private Trace getTraceToState(Trace trace, State state) {
		if (trace.getCurrentState().equals(state)) {
			return trace;
		} else if (trace.canGoBack()) {
			return getTraceToState(trace.back(), state);
		} else {
			return null;
		}
	}

	private void loadMachineImages(final Map<Integer, String> imageNames) {
		final Path projectDirectory = currentProject.get().getLocation();
		final Path machineFile = currentProject.getCurrentMachine().getPath();
		final Path machineDirectory = projectDirectory.resolve(machineFile).getParent();
		final Path proBDirectory = Paths.get(Main.getProBDirectory());
		final List<Path> imageDirectories = Arrays.asList(machineDirectory, projectDirectory, proBDirectory);
		
		final List<String> notFoundImages = new ArrayList<>();
		imageNames.forEach((number, name) -> {
			boolean found = false;
			for (final Path dir : imageDirectories) {
				final Path imagePath = dir.resolve(name);
				if (imagePath.toFile().exists()) {
					this.machineImages.put(number, new Image(imagePath.toUri().toString()));
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
	}
}
