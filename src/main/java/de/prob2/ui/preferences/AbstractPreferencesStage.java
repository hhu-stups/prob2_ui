package de.prob2.ui.preferences;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public abstract class AbstractPreferencesStage extends Stage {
	@FXML 
	protected Button undoButton;
	
	@FXML 
	protected Button resetButton;
	
	@FXML 
	protected Button applyButton;
	
	@FXML 
	protected Label applyWarning;
	
	protected final ProBPreferences globalProBPrefs;
	
	protected final PreferencesHandler preferencesHandler;
	
	@Inject
	protected AbstractPreferencesStage(final ProBPreferences globalProBPrefs, final PreferencesHandler preferencesHandler) {
		this.globalProBPrefs = globalProBPrefs;
		this.preferencesHandler = preferencesHandler;
		this.preferencesHandler.setPreferences(this.globalProBPrefs);
	}

	
	@FXML
	protected void initialize() {
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
		preferencesHandler.rollback();
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
