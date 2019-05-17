package de.prob2.ui.preferences;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final StageManager stageManager;
	
	protected final ProBPreferences globalProBPrefs;
	
	@Inject
	protected AbstractPreferencesStage(final StageManager stageManager, final ProBPreferences globalProBPrefs) {
		this.stageManager = stageManager;
		this.globalProBPrefs = globalProBPrefs;
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
		this.globalProBPrefs.rollback();
	}

	@FXML
	protected void handleRestoreDefaults() {
		for (ProBPreference pref : this.globalProBPrefs.getPreferences().values()) {
			this.globalProBPrefs.setPreferenceValue(pref.name, pref.defaultValue);
		}
	}
	
	@FXML
	protected void handleApply() {
		try {
			this.globalProBPrefs.apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content").show();
		}
	}
	
}
