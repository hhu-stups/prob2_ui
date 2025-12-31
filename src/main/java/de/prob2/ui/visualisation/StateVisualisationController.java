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
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class StateVisualisationController {
	@FXML
	private GridPane matrixPane;

	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final BooleanProperty visualisationPossible = new SimpleBooleanProperty(false);
	private final Map<Integer, Image> machineImages;

	@Inject
	public StateVisualisationController(final I18n i18n, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.machineImages = new HashMap<>();
	}

	public BooleanProperty visualisationPossibleProperty() {
		return visualisationPossible;
	}

	public Map<Integer, Image> getMachineImages() {
		return this.machineImages;
	}

	public void showMatrix(final Trace trace, final List<List<AnimationMatrixEntry>> matrix) {
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
					view.setOnContextMenuRequested(e -> getContextMenu(trace, entry)
						.show(view, e.getScreenX(), e.getScreenY()));
					matrixPane.add(view, c, r);
				}
			}
		}
	}

	private ContextMenu getContextMenu(Trace trace, AnimationMatrixEntry entry) {
		ContextMenu contextMenu = new ContextMenu();
		StateSpace stateSpace = trace.getStateSpace();
		GetRightClickOptionsForStateVisualizationCommand getOptionsCommand = new GetRightClickOptionsForStateVisualizationCommand(
				trace.getCurrentState().getId(), entry.getRow(), entry.getColumn());
		stateSpace.execute(getOptionsCommand);
		List<GetRightClickOptionsForStateVisualizationCommand.Option> options = getOptionsCommand.getOptionsWithDescription();
		for (GetRightClickOptionsForStateVisualizationCommand.Option opt : options) {
			String term = opt.getTransitionTerm();
			String desc = opt.getDescription();
			final MenuItem item = new MenuItem(desc.isEmpty() ? term : desc);
			item.setOnAction(e -> {
				ExecuteRightClickCommand executeCommand = new ExecuteRightClickCommand(trace.getCurrentState().getId(), entry.getRow(), entry.getColumn(), term);
				stateSpace.execute(executeCommand);
				Trace newTrace = trace;
				for (String transitionId : executeCommand.getTransitionIDs()) {
					newTrace = newTrace.add(transitionId);
				}
				currentTrace.set(newTrace);
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

	public void visualiseState(final Trace trace) {
		final List<List<AnimationMatrixEntry>> matrix;
		if (trace == null) {
			matrix = Collections.emptyList();
		} else {
			final GetAnimationMatrixForStateCommand cmd = new GetAnimationMatrixForStateCommand(trace.getCurrentState());
			trace.getStateSpace().execute(cmd);
			matrix = cmd.getMatrix();
		}
		
		Platform.runLater(() -> {
			matrixPane.getChildren().clear();
			final boolean it = !matrix.isEmpty();
			this.visualisationPossibleProperty().set(it);
			if (it) {
				this.showMatrix(trace, matrix);
			}
		});
	}
}
