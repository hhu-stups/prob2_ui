package de.prob2.ui.dynamic;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Provider;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.BackgroundUpdater;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicCommandStage<T extends DynamicCommandItem> extends Stage {
	private static final class DynamicCommandItemCell<T extends DynamicCommandItem> extends ListCell<T> {
		private DynamicCommandItemCell() {
			super();
			getStyleClass().add("dynamic-command-cell");
		}
		
		@Override
		protected void updateItem(final T item, final boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("disabled");
			if (item != null && !empty) {
				setText(item.getName());
				if (!item.isAvailable()) {
					getStyleClass().add("disabled");
				}
			}
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCommandStage.class);
	
	@FXML
	protected ListView<T> lvChoice;

	@FXML
	protected TextArea taFormula;
	
	@FXML
	protected TextArea taErrors;

	@FXML
	protected VBox enterFormulaBox;

	@FXML
	protected Label lbDescription;

	@FXML
	protected Button cancelButton;
	
	@FXML
	protected Button editPreferencesButton;
	
	@FXML
	protected Label placeholderLabel;
	
	@FXML
	protected DynamicCommandStatusBar statusBar;
	
	// Used to remember the last selected item even when the list might be cleared temporarily,
	// e. g. when reloading the current machine.
	protected T lastItem;
	
	private final Provider<DynamicPreferencesStage> preferencesStageProvider;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ResourceBundle bundle;
	
	protected final BackgroundUpdater updater;
	
	protected DynamicCommandStage(final Provider<DynamicPreferencesStage> preferencesStageProvider,
			final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle, final StopActions stopActions, final String threadName) {
		this.preferencesStageProvider = preferencesStageProvider;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.updater = new BackgroundUpdater(threadName);
		stopActions.add(this.updater::shutdownNow);
	}
	
	
	@FXML
	protected void initialize() {
		this.refresh();

		this.showingProperty().addListener((observable, from, to) -> {
			if(!from && to) {
				T choice = lvChoice.getSelectionModel().getSelectedItem();
				if(choice != null) {
					visualize(choice);
				}
			}
		});

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.updatePlaceholderLabel();
			if (to == null || currentTrace.get() == null || !this.isShowing()) {
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			// Update the visualization automatically if possible.
			// If the command selection changed and the new command requires a formula,
			// clear the visualization and wait for the user to input one.
			if (to.isAvailable() && (!needFormula || to.equals(lastItem))) {
				visualize(to);
			} else {
				this.interrupt();
			}
			lastItem = to;
		});
		lvChoice.disableProperty().bind(this.updater.runningProperty().or(currentTrace.stateSpaceProperty().isNull()));

		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.addStatesCalculatedListener(newOps -> Platform.runLater(this::refresh));

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			this.interrupt();
			lvChoice.getSelectionModel().clearSelection();
			this.lastItem = null;
			this.refresh();
		});
		
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					T item = lvChoice.getSelectionModel().getSelectedItem();
					if (item == null) {
						return;
					}
					visualize(item);
					e.consume();
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			}
		});
		lvChoice.setCellFactory(item -> new DynamicCommandItemCell<>());
		cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final T item = lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, lvChoice.getSelectionModel().selectedItemProperty()));
	}
	
	@FXML
	private void editPreferences() {
		final DynamicPreferencesStage preferences = this.preferencesStageProvider.get();
		preferences.initOwner(this);
		preferences.initModality(Modality.WINDOW_MODAL);
		preferences.setToRefresh(this);
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setIncludedPreferenceNames(currentItem.getRelevantPreferences());
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}
	
	@FXML
	protected void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		interrupt();
	}
	
	private void updatePlaceholderLabel() {
		final String text;
		if (this.updater.isRunning()) {
			text = bundle.getString("dynamic.placeholder.inProgress");
		} else if (currentTrace.get() == null) {
			text = bundle.getString("common.noModelLoaded");
		} else {
			final T selectedItem = lvChoice.getSelectionModel().getSelectedItem();
			if (selectedItem == null) {
				text = bundle.getString("dynamic.placeholder.selectVisualization");
			} else if (selectedItem.getArity() > 0) {
				text = bundle.getString("dynamic.enterFormula.placeholder");
			} else {
				// The placeholder label shouldn't be seen by the user in this case,
				// because the visualization content should be visible,
				// but clear the text anyway just in case.
				text = "";
			}
		}
		placeholderLabel.setText(text);
	}
	
	public void refresh() {
		this.updatePlaceholderLabel();
		int index = lvChoice.getSelectionModel().getSelectedIndex();
		final State currentState = currentTrace.getCurrentState();
		if (currentState == null) {
			lvChoice.getItems().clear();
		} else {
			lvChoice.getItems().setAll(this.getCommandsInState(currentState));
		}
		if (index == -1) {
			if(this.lastItem != null) {
				lvChoice.getSelectionModel().select(this.lastItem);
			}
		} else {
			lvChoice.getSelectionModel().select(index);
		}
	}
	
	protected void interrupt() {
		this.updater.cancel(true);
		this.clearLoadingStatus();
		this.clearContent();
		this.updatePlaceholderLabel();
	}
	
	protected void clearLoadingStatus() {
		statusBar.setText("");
		statusBar.removeLabelStyle("warning");
	}
	
	protected abstract void clearContent();
	
	protected void visualize(final T item) {
		if (!item.isAvailable()) {
			return;
		}
		interrupt();

		this.updater.execute(() -> {
			Platform.runLater(() -> {
				this.updatePlaceholderLabel();
				statusBar.setText(bundle.getString("statusbar.loadStatus.loading"));
			});
			try {
				final Trace trace = currentTrace.get();
				if(trace == null || (item.getArity() > 0 && taFormula.getText().isEmpty())) {
					Platform.runLater(this::clearLoadingStatus);
					return;
				}
				final List<IEvalElement> formulas;
				if (item.getArity() > 0) {
					formulas = Collections.singletonList(trace.getModel().parseFormula(taFormula.getText(), FormulaExpand.EXPAND));
				} else {
					formulas = Collections.emptyList();
				}
				visualizeInternal(item, formulas);
			} catch (CommandInterruptedException | InterruptedException e) {
				LOGGER.info("Visualization interrupted", e);
				Thread.currentThread().interrupt();
				Platform.runLater(this::clearLoadingStatus);
			} catch (ProBError | EvaluationException e) {
				LOGGER.error("Visualization failed", e);
				Platform.runLater(() -> {
					taErrors.setText(e.getMessage());
					this.clearLoadingStatus();
				});
			}
		});
	}
	
	protected abstract void visualizeInternal(final T item, final List<IEvalElement> formulas) throws InterruptedException;
	
	protected abstract List<T> getCommandsInState(final State state);

	public void selectCommand(final String command, final String formula) {
		final T choice = lvChoice.getItems().stream()
			.filter(item -> command.equals(item.getCommand()))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Visualization command not found: " + command));
		lvChoice.getSelectionModel().select(choice);
		if (formula != null) {
			if (choice.getArity() == 0) {
				throw new IllegalArgumentException("Visualization command does not take an argument: " + command);
			}
			taFormula.setText(formula);
			visualize(choice);
		}
	}

	public void selectCommand(final String command) {
		this.selectCommand(command, null);
	}
}
