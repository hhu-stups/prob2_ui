package de.prob2.ui.dynamic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Injector;

import de.prob.animator.command.AbstractGetDynamicCommands;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesHandler;
import de.prob2.ui.preferences.ProBPreferenceType;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.preferences.PreferencesView;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicCommandStage extends Stage {
	private static final class DynamicCommandItemCell extends ListCell<DynamicCommandItem> {
		private DynamicCommandItemCell() {
			super();
			getStyleClass().add("dynamic-command-cell");
		}
		
		@Override
		protected void updateItem(final DynamicCommandItem item, final boolean empty) {
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
	protected ListView<DynamicCommandItem> lvChoice;

	@FXML
	protected TextArea taFormula;

	@FXML
	protected VBox enterFormulaBox;

	@FXML
	protected Label lbDescription;

	@FXML
	protected CheckBox cbContinuous;

	@FXML
	protected ScrollPane pane;
	
	@FXML
	protected Button cancelButton;
	
	@FXML
	protected Button applyButton;
	
	@FXML
	protected Button resetButton;
	
	@FXML
	protected Button undoButton;
	
	@FXML
	protected Label applyWarning;
	
	@FXML
	protected DynamicCommandStatusBar statusBar;
	
	@FXML
	protected DynamicPreferencesTableView preferences;
	
	protected DynamicCommandItem lastItem;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ResourceBundle bundle;
	
	protected final StageManager stageManager;
	
	protected final ObjectProperty<Thread> currentThread;
	
	protected final Injector injector;
	
	private final GlobalPreferences globalPreferences;
	
	private final ProBPreferences globalProBPrefs;
	
	private final PreferencesHandler preferencesHandler;
	
	protected DynamicCommandStage(final StageManager stageManager, final CurrentTrace currentTrace, 
			final CurrentProject currentProject, final ProBPreferences globalProBPrefs, final GlobalPreferences globalPreferences,
			final MachineLoader machineLoader, final PreferencesHandler preferencesHandler,
			final ResourceBundle bundle, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.globalPreferences = globalPreferences;
		this.globalProBPrefs = globalProBPrefs;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace());
		this.injector = injector;
		this.bundle = bundle;
		this.preferencesHandler = preferencesHandler;
		this.stageManager = stageManager;
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
	}
	
	
	@FXML
	protected void initialize() {
		fillCommands();
		currentTrace.addListener((observable, from, to) -> {
			preferences.getItems().clear();
			injector.getInstance(PreferencesView.class).refresh();
			if(to == null || lvChoice.getSelectionModel().getSelectedItem() == null) {
				return;
			}
			updatePreferences(lvChoice.getSelectionModel().getSelectedItem().getRelevantPreferences());
		});
		
		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			preferences.getItems().clear();
			if (to == null) {
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			updatePreferences(to.getRelevantPreferences());		
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			String currentFormula = taFormula.getText();
			if(lastItem != null && !lastItem.getCommand().equals(to.getCommand())) {
				reset();
			}
			if ((!needFormula || !currentFormula.isEmpty()) && (lastItem == null
					|| !Objects.equals(lastItem.getCommand(), to.getCommand()) || cbContinuous.isSelected())) {
				visualize(to);
			}
			lastItem = to;
		});
		lvChoice.disableProperty().bind(currentThread.isNotNull());
		
		currentTrace.currentStateProperty().addListener((observable, from, to) -> refresh());
		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.stateSpaceProperty().addListener((observable, from, to) -> refresh());
		injector.getInstance(Modelchecker.class).resultProperty().addListener((observable, from, to) -> refresh());
		
		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			fillCommands();
			reset();
		});
		
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
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
		lvChoice.setCellFactory(item -> new DynamicCommandItemCell());
		cancelButton.disableProperty().bind(currentThread.isNull());
		
		this.globalPreferences.addListener((InvalidationListener) observable -> {
			for (final Map.Entry<String, String> entry : this.globalPreferences.entrySet()) {
				this.globalProBPrefs.setPreferenceValue(entry.getKey(), entry.getValue());
			}

			try {
				this.globalProBPrefs.apply();
			} catch (final ProBError e) {
				LOGGER.warn("Ignoring global preference changes because of exception", e);
			}
		});
		this.globalPreferences.addListener((MapChangeListener<String, String>) change -> {
			if (change.wasRemoved() && !change.wasAdded()) {
				this.globalProBPrefs.setPreferenceValue(change.getKey(), this.globalProBPrefs.getPreferences().get(change.getKey()).defaultValue);
				this.globalProBPrefs.apply();
			}
		});
		
		this.undoButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		this.applyWarning.visibleProperty().bind(this.globalProBPrefs.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		
		// prevent text on buttons from being abbreviated
		undoButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		applyButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		resetButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
	}
	
	private void updatePreferences(List<String> relevantPreferences) {
		GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
		currentTrace.getStateSpace().execute(cmd);
		GetDefaultPreferencesCommand cmd2 = new GetDefaultPreferencesCommand();
		currentTrace.getStateSpace().execute(cmd2);
		preferences.getItems().addAll(cmd2.getPreferences().stream()
				.filter(preference -> relevantPreferences.contains(preference.name))
				.map(preference -> new PrefItem(preference.name, "", preference.defaultValue, ProBPreferenceType.fromProBPreference(preference), preference.defaultValue, preference.description))
				.collect(Collectors.toList()));
		preferences.getItems().forEach(preference -> {
			String value = cmd.getPreferences().get(preference.getName());
			preference.setValue(value);
			preference.setChanged(value.equals(preference.getDefaultValue()) ? "" : "*");
		});
		if(preferences.getItems().isEmpty()) {
			preferences.setVisible(false);
		} else {
			preferences.setVisible(true);
		}
	}
	
	protected void fillCommands(AbstractGetDynamicCommands cmd) {
		if(currentTrace.get() == null) {
			return;
		}
		try {
			lvChoice.getItems().clear();
			currentTrace.getStateSpace().execute(cmd);
			lvChoice.getItems().setAll(cmd.getCommands());
		} catch (ProBError | CliError e) {
			LOGGER.error("Extract all expression table commands failed", e);
		}
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
			lvChoice.getSelectionModel().select(this.lastItem);
		} else {
			lvChoice.getSelectionModel().select(index);
		}
		preferences.refresh();
	}
	
	protected void interrupt() {
		if (currentThread.get() != null) {
			currentThread.get().interrupt();
			currentThread.set(null);
		}
		reset();
	}
	
	protected abstract void reset();
	
	protected abstract void visualize(DynamicCommandItem item);
	
	protected abstract void fillCommands();
	
	
	@FXML
	private void handleUndoChanges() {
		preferencesHandler.undo();
	}

	@FXML
	private void handleRestoreDefaults() {
		preferencesHandler.restoreDefaults();
	}
	
	@FXML
	private void handleApply() {
		preferencesHandler.apply();
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
}
