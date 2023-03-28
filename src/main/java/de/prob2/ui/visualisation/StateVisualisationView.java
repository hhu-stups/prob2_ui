package de.prob2.ui.visualisation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.ExecuteRightClickCommand;
import de.prob.animator.command.GetAnimationMatrixForStateCommand;
import de.prob.animator.command.GetRightClickOptionsForStateVisualizationCommand;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

@FXMLInjected
public class StateVisualisationView extends GridPane {
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private BooleanProperty visualisationPossible = new SimpleBooleanProperty(false);
	private final Map<Integer, Image> machineImages;

	@Inject
	public StateVisualisationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.machineImages = new HashMap<>();
		stageManager.loadFXML(this, "state_visualisation_view.fxml");
	}

	public BooleanProperty visualisationPossibleProperty() {
		return visualisationPossible;
	}

	public Map<Integer, Image> getMachineImages() {
		return this.machineImages;
	}

	public void showMatrix(final State state, final List<List<AnimationMatrixEntry>> matrix) {
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
					this.add(view, c, r);
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
			final MenuItem item = new MenuItem(i18n.translate("visualisation.stateVisualisationView.contextMenu.noRightClickOptions"));
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

	public void visualiseState(final State state) {
		final List<List<AnimationMatrixEntry>> matrix;
		if (state == null) {
			matrix = Collections.emptyList();
		} else {
			final GetAnimationMatrixForStateCommand cmd = new GetAnimationMatrixForStateCommand(state);
			state.getStateSpace().execute(cmd);
			matrix = cmd.getMatrix();
		}
		
		Platform.runLater(() -> {
			this.getChildren().clear();
			final boolean it = !matrix.isEmpty();
			this.visualisationPossibleProperty().set(it);
			if (it) {
				this.showMatrix(state, matrix);
			}
		});
	}
}
