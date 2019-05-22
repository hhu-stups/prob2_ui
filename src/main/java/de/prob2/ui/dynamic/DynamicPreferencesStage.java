package de.prob2.ui.dynamic;

import java.util.Collection;
import java.util.HashSet;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.AbstractPreferencesStage;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;

public class DynamicPreferencesStage extends AbstractPreferencesStage {
	
	@FXML
	protected PreferencesView preferences;
	
	@Inject
	private DynamicPreferencesStage(final StageManager stageManager, final ProBPreferences globalProBPrefs, final CurrentTrace currentTrace) {
		super(stageManager, globalProBPrefs);
		this.globalProBPrefs.stateSpaceProperty().bind(currentTrace.stateSpaceProperty());
		stageManager.loadFXML(this, "dynamic_preferences_view.fxml");
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
		this.preferences.setPreferences(this.globalProBPrefs);
	}
	
	public void setIncludedPreferenceNames(final Collection<String> preferenceNames) {
		this.globalProBPrefs.setIncludedPreferenceNames(FXCollections.observableSet(new HashSet<>(preferenceNames)));
	}
	
	@Override
	protected void handleUndoChanges() {
		super.handleUndoChanges();
		this.preferences.refresh();
	}
	
	@Override
	protected void handleRestoreDefaults() {
		super.handleRestoreDefaults();
		this.preferences.refresh();
	}
}
