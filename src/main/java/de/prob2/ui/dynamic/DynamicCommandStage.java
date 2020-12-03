package de.prob2.ui.dynamic;

import java.util.List;
import java.util.ResourceBundle;

import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
	
	protected final StageManager stageManager;
	
	protected final ObjectProperty<Thread> currentThread;
	
	protected DynamicCommandStage(final StageManager stageManager, final DynamicPreferencesStage preferences,
			final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle) {
		this.preferences = preferences;
		this.preferences.initOwner(this);
		this.preferences.initModality(Modality.WINDOW_MODAL);
		this.preferences.setToRefresh(this);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
	}
	
	
	@FXML
	protected void initialize() {
		fillCommands();
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
		lvChoice.disableProperty().bind(currentThread.isNotNull().or(currentTrace.stateSpaceProperty().isNull()));

		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.addStatesCalculatedListener(newOps -> Platform.runLater(this::refresh));

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			fillCommands();
			lvChoice.getSelectionModel().clearSelection();
			this.lastItem = null;
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
		cancelButton.disableProperty().bind(currentThread.isNull());
		editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final T item = lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, lvChoice.getSelectionModel().selectedItemProperty()));
	}
	
	protected void fillCommands() {
		final State currentState = currentTrace.getCurrentState();
		if (currentState == null) {
			return;
		}
		lvChoice.getItems().setAll(this.getCommandsInState(currentState));
	}
	
	@FXML
	protected void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		interrupt();
	}
	
	public void refresh() {
		int index = lvChoice.getSelectionModel().getSelectedIndex();
		fillCommands();
		if (index == -1) {
			if(this.lastItem != null) {
				lvChoice.getSelectionModel().select(this.lastItem);
			}
		} else {
			lvChoice.getSelectionModel().select(index);
		}
	}
	
	protected void interrupt() {
		if (currentThread.get() != null) {
			currentThread.get().interrupt();
			currentThread.set(null);
		}
		reset();
	}
	
	protected abstract void reset();
	
	protected abstract void visualize(T item);
	
	protected abstract List<T> getCommandsInState(final State state);

}
