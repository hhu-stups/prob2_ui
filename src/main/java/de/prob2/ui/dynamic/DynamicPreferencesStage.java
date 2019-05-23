package de.prob2.ui.dynamic;

import java.util.Collection;
import java.util.HashSet;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicPreferencesStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicPreferencesStage.class);
	
	@FXML
	private Button resetButton;
	
	@FXML
	private Button cancelButton;
	
	@FXML
	private Button okButton;
	
	@FXML
	protected PreferencesView preferences;
	
	private final StageManager stageManager;
	private final ProBPreferences proBPreferences;
	
	private DynamicCommandStage toRefresh;
	
	@Inject
	private DynamicPreferencesStage(final StageManager stageManager, final ProBPreferences proBPreferences, final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.proBPreferences = proBPreferences;
		
		this.proBPreferences.stateSpaceProperty().bind(currentTrace.stateSpaceProperty());
		stageManager.loadFXML(this, "dynamic_preferences_view.fxml");
	}
	
	@FXML
	private void initialize() {
		// prevent text on buttons from being abbreviated
		resetButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		cancelButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		okButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		
		this.preferences.setPreferences(this.proBPreferences);
	}
	
	public void setToRefresh(final DynamicCommandStage toRefresh) {
		this.toRefresh = toRefresh;
	}
	
	public void setIncludedPreferenceNames(final Collection<String> preferenceNames) {
		this.proBPreferences.setIncludedPreferenceNames(FXCollections.observableSet(new HashSet<>(preferenceNames)));
	}
	
	@FXML
	private void handleRestoreDefaults() {
		this.proBPreferences.restoreDefaults();
		this.preferences.refresh();
	}
	
	@FXML
	private void handleCancel() {
		this.proBPreferences.rollback();
		this.preferences.refresh();
		this.close();
	}
	
	@FXML
	private void handleOk() {
		try {
			this.proBPreferences.apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content").show();
			return;
		}
		this.toRefresh.refresh();
		this.close();
	}
}
