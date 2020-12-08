package de.prob2.ui.dynamic;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

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
import javafx.scene.control.ScrollPane;
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
			this.getStyleClass().removeAll("dynamiccommandenabled", "dynamiccommanddisabled");
			if (item != null && !empty) {
				setText(item.getName());
				if (item.isAvailable()) {
					getStyleClass().add("dynamiccommandenabled");
				} else {
					getStyleClass().add("dynamiccommanddisabled");
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
	protected ScrollPane pane;

	@FXML
	protected Button cancelButton;
	
	@FXML
	protected Button editPreferencesButton;
	
	@FXML
	protected DynamicCommandStatusBar statusBar;
	
	protected T lastItem;
	
	protected final DynamicPreferencesStage preferences;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ResourceBundle bundle;
	
	protected final BackgroundUpdater updater;
	
	protected DynamicCommandStage(final DynamicPreferencesStage preferences,
			final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle, final StopActions stopActions, final String threadName) {
		this.preferences = preferences;
		this.preferences.initOwner(this);
		this.preferences.initModality(Modality.WINDOW_MODAL);
		this.preferences.setToRefresh(this);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.updater = new BackgroundUpdater(threadName);
		stopActions.add(this.updater::shutdownNow);
	}
	
	
	@FXML
	protected void initialize() {
		this.refresh();
		currentTrace.addListener((observable, from, to) -> {
			if(to == null || lvChoice.getSelectionModel().getSelectedItem() == null) {
				return;
			}
			preferences.setIncludedPreferenceNames(lvChoice.getSelectionModel().getSelectedItem().getRelevantPreferences());
		});

		this.showingProperty().addListener((observable, from, to) -> {
			if(!from && to) {
				T choice = lvChoice.getSelectionModel().getSelectedItem();
				if(choice != null) {
					visualize(choice);
				}
			}
		});

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (to == null || currentTrace.get() == null || !this.isShowing()) {
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			preferences.setIncludedPreferenceNames(to.getRelevantPreferences());
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			if(lastItem != null && !lastItem.getCommand().equals(to.getCommand())) {
				reset();
			}
			//only visualize if
			//1. No formula is needed and command is changed or continuous update is selected
			//2. Formula is needed and command is not changed and continuous update is selected
			if (!needFormula || to.equals(lastItem)) {
				visualize(to);
			}
			lastItem = to;
		});
		lvChoice.disableProperty().bind(this.updater.runningProperty().or(currentTrace.stateSpaceProperty().isNull()));

		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.addStatesCalculatedListener(newOps -> Platform.runLater(this::refresh));

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			lvChoice.getSelectionModel().clearSelection();
			this.lastItem = null;
			this.refresh();
			reset();
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
	protected void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		interrupt();
	}
	
	public void refresh() {
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
		reset();
	}
	
	protected abstract void reset();
	
	protected void visualize(final T item) {
		if (!item.isAvailable()) {
			return;
		}
		interrupt();

		this.updater.execute(() -> {
			Platform.runLater(()-> statusBar.setText(bundle.getString("statusbar.loadStatus.loading")));
			try {
				final Trace trace = currentTrace.get();
				if(trace == null || (item.getArity() > 0 && taFormula.getText().isEmpty())) {
					Platform.runLater(this::reset);
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
				Platform.runLater(this::reset);
			} catch (ProBError | EvaluationException e) {
				LOGGER.error("Visualization failed", e);
				Platform.runLater(() -> {
					taErrors.setText(e.getMessage());
					this.reset();
					statusBar.setText("");
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
