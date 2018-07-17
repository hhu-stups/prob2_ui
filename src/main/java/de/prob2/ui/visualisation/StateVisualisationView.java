package de.prob2.ui.visualisation;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.ExecuteRightClickCommand;
import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetImagesForMachineCommand;
import de.prob.animator.command.GetRightClickOptionsForStateVisualizationCommand;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class StateVisualisationView extends AnchorPane {
	@FXML
	private GridPane visualisationGridPane;

	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private BooleanProperty visualisationPossible = new SimpleBooleanProperty(false);

	@Inject
	public StateVisualisationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "state_visualisation_view.fxml");
	}

	public void visualiseState(State state) throws FileNotFoundException {
		visualisationGridPane.getChildren().clear();
		visualisationPossible.set(false);
		if (state == null || !state.isInitialised() || currentProject.getCurrentMachine() == null) {
			return;
		}
		StateSpace stateSpace = state.getStateSpace();
		GetImagesForMachineCommand getImagesForMachineCommand = new GetImagesForMachineCommand();
		stateSpace.execute(getImagesForMachineCommand);
		Map<Integer, String> images = getImagesForMachineCommand.getImages();
		showMatrix(state, images);
	}

	public BooleanProperty visualisationPossibleProperty() {
		return visualisationPossible;
	}

	private void showMatrix(State state, Map<Integer, String> images) throws FileNotFoundException {
		final GetAnimationMatrixForStateCommand cmd = new GetAnimationMatrixForStateCommand(state);
		state.getStateSpace().execute(cmd);
		final List<List<Object>> matrix = cmd.getMatrix();
		if (matrix.isEmpty()) {
			visualisationPossible.set(false);
			return;
		} else {
			visualisationPossible.set(true);
		}

		for (int r = 0; r < matrix.size(); r++) {
			final List<Object> row = matrix.get(r);
			for (int c = 0; c < row.size(); c++) {
				final Object entry = row.get(c);
				final Node view;
				if (entry == null) {
					view = null;
				} else if (entry instanceof Integer) {
					view = new ImageView(getImage(images.get(entry)));
				} else if (entry instanceof String) {
					view = new Label((String)entry);
				} else {
					throw new AssertionError("Unhandled animation matrix entry type: " + entry.getClass());
				}

				if (view != null) {
					final int finalRow = r;
					final int finalColumn = c;
					view.setOnContextMenuRequested(e -> getContextMenu(state, finalRow, finalColumn)
						.show(view, e.getScreenX(), e.getScreenY()));
					visualisationGridPane.add(view, c, r);
				}
			}
		}
	}

	private ContextMenu getContextMenu(State state, int row, int column) {
		ContextMenu contextMenu = new ContextMenu();
		StateSpace stateSpace = state.getStateSpace();
		GetRightClickOptionsForStateVisualizationCommand getOptionsCommand = new GetRightClickOptionsForStateVisualizationCommand(
				state.getId(), row, column);
		stateSpace.execute(getOptionsCommand);
		List<String> options = getOptionsCommand.getOptions();
		for (String opt : options) {
			final MenuItem item = new MenuItem(opt);
			Trace trace = getTraceToState(currentTrace.get(), state);
			if (trace == null) {
				item.setDisable(true);
			}
			item.setOnAction(e -> {
				ExecuteRightClickCommand executeCommand = new ExecuteRightClickCommand(state.getId(), row, column, opt);
				stateSpace.execute(executeCommand);
				String transitionId = executeCommand.getTransitionID();
				currentTrace.set(trace.add(transitionId));
			});
			contextMenu.getItems().add(item);
		}
		if (options.isEmpty()) {
			final MenuItem item = new MenuItem(bundle.getString("visualisation.noRightClickOptions"));
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

	private Image getImage(String imageURL) throws FileNotFoundException {
		final Path projectFolder = currentProject.get().getLocation();
		// look in machine folder
		final Path machineFile = currentProject.getCurrentMachine().getPath();
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
					throw new FileNotFoundException(String.format(bundle.getString("visualisation.error.imageNotFound"), imagePath.getName(), imageInMachineFolder.getParent(), imageInProjectFolder.getParent(), imageInProbFolder.getParent()));
				}
			}
		}
		return new Image(imagePath.toURI().toString());
	}
}
