package de.prob2.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.ModelTranslationError;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class PreferencesStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesStage.class);

	@FXML private Spinner<Integer> recentProjectsCountSpinner;
	@FXML private TextField defaultLocationField;
	@FXML private PreferencesView globalPrefsView;
	@FXML private Button undoButton;
	@FXML private Button resetButton;
	@FXML private Button applyButton;
	@FXML private Label applyWarning;
	@FXML private TabPane tabPane;
	@FXML private Tab tabGeneral;
	@FXML private Tab tabPreferences;

	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final ProBPreferences globalProBPrefs;
	private final RecentProjects recentProjects;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final StringProperty currentTab;

	@Inject
	private PreferencesStage(
		final CurrentTrace currentTrace,
		final GlobalPreferences globalPreferences,
		final ProBPreferences globalProBPrefs,
		final MachineLoader machineLoader,
		final RecentProjects recentProjects,
		final StageManager stageManager,
		final CurrentProject currentProject
	) {
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.globalProBPrefs = globalProBPrefs;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace(this.globalPreferences));
		this.recentProjects = recentProjects;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTab = new SimpleStringProperty(this, "currentTab", null);

		stageManager.loadFXML(this, "preferences_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		// General
		
		final SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
		
		// bindBidirectional doesn't work properly here, don't ask why
		this.recentProjects.maximumProperty().addListener((observable, from, to) -> valueFactory.setValue((Integer)to));
		valueFactory.valueProperty().addListener((observable, from, to) -> this.recentProjects.setMaximum(to));
		valueFactory.setValue(this.recentProjects.getMaximum());
		
		this.recentProjectsCountSpinner.setValueFactory(valueFactory);
		
		defaultLocationField.setText(this.currentProject.getDefaultLocation().toString());
		defaultLocationField.textProperty().addListener((observable, from, to) -> this.currentProject.setDefaultLocation(Paths.get(to)));

		// Global Preferences
		
		this.globalPrefsView.setPreferences(this.globalProBPrefs);
		
		this.globalPreferences.addListener((InvalidationListener)observable -> {
			for (final Map.Entry<String, String> entry : this.globalPreferences.entrySet()) {
				this.globalProBPrefs.setPreferenceValue(entry.getKey(), entry.getValue());
			}
			
			try {
				this.globalProBPrefs.apply();
			} catch (final ProBError e) {
				LOGGER.warn("Ignoring global preference changes because of exception", e);
			}
		});
		this.globalPreferences.addListener((MapChangeListener<String, String>)change -> {
			if (change.wasRemoved() && !change.wasAdded()) {
				this.globalProBPrefs.setPreferenceValue(change.getKey(), this.globalProBPrefs.getPreferences().get(change.getKey()).defaultValue);
				this.globalProBPrefs.apply();
			}
		});
		
		this.undoButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		this.applyWarning.visibleProperty().bind(this.globalProBPrefs.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		
		this.currentTabProperty().addListener((observable, from, to) -> {
			switch (to) {
				case "general":
					this.tabPane.getSelectionModel().select(this.tabGeneral);
					break;
				
				case "preferences":
					this.tabPane.getSelectionModel().select(this.tabPreferences);
					break;
				
				default:
					LOGGER.warn("Attempted to select unknown preferences tab: {}", to);
			}
		});
		this.tabPane.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.setCurrentTab(to.getId()));
		this.setCurrentTab(this.tabPane.getSelectionModel().getSelectedItem().getId());
	}
	
	@FXML
	private void selectDefaultLocation(ActionEvent event) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Select default location to store new projects");
		File file = dirChooser.showDialog(this.getOwner());
		if (file != null) {
			defaultLocationField.setText(file.getAbsolutePath());
		}
	}
	
	@FXML
	private void handleUndoChanges() {
		this.globalProBPrefs.rollback();
	}
	
	@FXML
	private void handleRestoreDefaults() {
		for (ProBPreference pref : this.globalProBPrefs.getPreferences().values()) {
			this.globalProBPrefs.setPreferenceValue(pref.name, pref.defaultValue);
		}
	}
	
	@FXML
	private void handleApply() {
		final Map<String, String> changed = new HashMap<>(this.globalProBPrefs.getChangedPreferences());
		
		try {
			this.globalProBPrefs.apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to apply preference changes:\n" + e).show();
		}
		
		final Map<String, ProBPreference> defaults = this.globalProBPrefs.getPreferences();
		for (final Map.Entry<String, String> entry : changed.entrySet()) {
			if (defaults.get(entry.getKey()).defaultValue.equals(entry.getValue())) {
				this.globalPreferences.remove(entry.getKey());
			} else {
				this.globalPreferences.put(entry.getKey(), entry.getValue());
			}
		}
		
		if (this.currentTrace.exists()) {
			try {
				this.currentTrace.reload(this.currentTrace.get(), this.globalPreferences);
			} catch (CliError | IOException | ModelTranslationError | ProBError e) {
				LOGGER.error("Failed to reload machine", e);
				this.stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to reload machine").show();
			}
		}
	}

	@FXML
	private void handleClose() {
		this.globalProBPrefs.rollback();
		this.hide();
	}
	
	public StringProperty currentTabProperty() {
		return this.currentTab;
	}
	
	public String getCurrentTab() {
		return this.currentTabProperty().get();
	}
	
	public void setCurrentTab(String tab) {
		this.currentTabProperty().set(tab);
	}
}
