package de.prob2.ui.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PreferencesChangeState;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
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
	private final GlobalPreferences globalPreferences;
	private final CurrentTrace currentTrace;

	private DynamicVisualizationStage toRefresh;
	
	@Inject
	private DynamicPreferencesStage(final StageManager stageManager, final GlobalPreferences globalPreferences, final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.globalPreferences = globalPreferences;
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "dynamic_preferences_view.fxml");
	}
	
	@FXML
	private void initialize() {
		// prevent text on buttons from being abbreviated
		resetButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		cancelButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		okButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
	}

	public void setToRefresh(final DynamicVisualizationStage toRefresh) {
		this.toRefresh = toRefresh;
	}
	
	public void setIncludedPreferenceNames(final Collection<String> preferenceNames) {
		final List<ProBPreference> visiblePreferences = new ArrayList<>(this.currentTrace.getStateSpace().getPreferenceInformation());
		visiblePreferences.removeIf(pref -> !preferenceNames.contains(pref.name));
		final PreferencesChangeState state = new PreferencesChangeState(visiblePreferences);
		this.globalPreferences.addListener((o, from, to) -> state.setCurrentPreferenceValues(to));
		state.setCurrentPreferenceValues(this.globalPreferences);
		this.preferences.setState(state);
	}
	
	@FXML
	private void handleRestoreDefaults() {
		this.preferences.getState().restoreDefaults();
		this.preferences.refresh();
	}
	
	@FXML
	private void handleCancel() {
		this.preferences.getState().rollback();
		this.preferences.refresh();
		this.close();
	}
	
	@FXML
	private void handleOk() {
		final Map<String, String> changedPreferences = new HashMap<>(this.preferences.getState().getPreferenceChanges());
		try {
			this.currentTrace.getStateSpace().changePreferences(changedPreferences);
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToApplyChanges.content");
			alert.initOwner(this);
			alert.show();
			return;
		}
		this.globalPreferences.get().putAll(changedPreferences);
		this.preferences.getState().apply();
		if (this.toRefresh != null) {
			this.toRefresh.refresh();
		}
		this.close();
	}
}
