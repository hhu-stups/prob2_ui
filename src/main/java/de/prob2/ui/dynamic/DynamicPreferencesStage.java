package de.prob2.ui.dynamic;

import java.util.List;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.AbstractPreferencesStage;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesHandler;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;

public class DynamicPreferencesStage extends AbstractPreferencesStage {
	
	@FXML
	protected DynamicPreferencesTableView preferences;
	
	@Inject
	private DynamicPreferencesStage(final StageManager stageManager, final ProBPreferences globalProBPrefs, final PreferencesHandler preferencesHandler, final CurrentTrace currentTrace) {
		super(globalProBPrefs, preferencesHandler);
		this.globalProBPrefs.stateSpaceProperty().bind(currentTrace.stateSpaceProperty());
		stageManager.loadFXML(this, "dynamic_preferences_view.fxml");
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
		this.preferences.setPreferences(this.globalProBPrefs);
	}
	
	public void clear() {
		preferences.getItems().clear();
	}
	
	
	public void refresh() {
		preferences.refresh();
	}
	
	public void addAll(List<PrefItem> items) {
		preferences.getItems().addAll(items);
	}
	
	@Override
	protected void handleUndoChanges() {
		super.handleUndoChanges();
		this.refresh();
	}
	
	@Override
	protected void handleRestoreDefaults() {
		super.handleRestoreDefaults();
		this.refresh();
	}
}
