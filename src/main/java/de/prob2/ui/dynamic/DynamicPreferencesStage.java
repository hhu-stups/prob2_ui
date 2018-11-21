package de.prob2.ui.dynamic;

import java.util.List;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.AbstractPreferencesStage;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesHandler;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;

public class DynamicPreferencesStage extends AbstractPreferencesStage {
	
	@FXML
	protected DynamicPreferencesTableView preferences;
	
	@Inject
	private DynamicPreferencesStage(final StageManager stageManager, final ProBPreferences globalProBPrefs, 
			final GlobalPreferences globalPreferences, final MachineLoader machineLoader, 
			final PreferencesHandler preferencesHandler) {
		super(globalProBPrefs, globalPreferences, preferencesHandler, machineLoader);
		stageManager.loadFXML(this, "dynamic_preferences_view.fxml");
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

}
