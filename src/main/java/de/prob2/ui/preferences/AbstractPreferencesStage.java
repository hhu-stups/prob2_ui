package de.prob2.ui.preferences;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.project.MachineLoader;
import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public abstract class AbstractPreferencesStage extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPreferencesStage.class);
	
	@FXML 
	protected Button undoButton;
	
	@FXML 
	protected Button resetButton;
	
	@FXML 
	protected Button applyButton;
	
	@FXML 
	protected Label applyWarning;
	
	protected final GlobalPreferences globalPreferences;
	
	protected final ProBPreferences globalProBPrefs;
	
	protected final PreferencesHandler preferencesHandler;
	
	@Inject
	protected AbstractPreferencesStage(final ProBPreferences globalProBPrefs, final GlobalPreferences globalPreferences,
										final PreferencesHandler preferencesHandler, final MachineLoader machineLoader) {
		this.globalProBPrefs = globalProBPrefs;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace());
		this.globalPreferences = globalPreferences;
		this.preferencesHandler = preferencesHandler;
	}

	
	@FXML
	protected void initialize() {
		// Global Preferences
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
	
	@FXML
	protected void handleUndoChanges() {
		preferencesHandler.undo();
	}

	@FXML
	protected void handleRestoreDefaults() {
		preferencesHandler.restoreDefaults();
	}
	
	@FXML
	protected void handleApply() {
		preferencesHandler.apply();
	}
	
}
