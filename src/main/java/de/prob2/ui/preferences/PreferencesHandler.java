package de.prob2.ui.preferences;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;

@Singleton
public class PreferencesHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesHandler.class);
	
	private final GlobalPreferences globalPreferences;
	
	private final ProBPreferences globalProBPrefs;
	
	private final CurrentProject currentProject;
	
	private final PreferencesView globalPrefsView;
	
	private final StageManager stageManager;
	
	private final Injector injector;
	
	@Inject
	private PreferencesHandler(final StageManager stageManager, final GlobalPreferences globalPreferences, 
			final ProBPreferences globalProBPrefs, final MachineLoader machineLoader, final PreferencesView globalPrefsView,
			final CurrentProject currentProject, final Injector injector) {
		this.globalPreferences = globalPreferences;
		this.globalProBPrefs = globalProBPrefs;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace());
		this.currentProject = currentProject;
		this.globalPrefsView = globalPrefsView;
		this.stageManager = stageManager;
		this.injector = injector;
	}

	public void apply() {
		final Map<String, String> changed = new HashMap<>(this.globalProBPrefs.getChangedPreferences());
		
		try {
			this.globalProBPrefs.apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content").show();
		}
		
		final Map<String, ProBPreference> defaults = this.globalProBPrefs.getPreferences();
		for (final Map.Entry<String, String> entry : changed.entrySet()) {
			if (defaults.get(entry.getKey()).defaultValue.equals(entry.getValue())) {
				this.globalPreferences.remove(entry.getKey());
			} else {
				this.globalPreferences.put(entry.getKey(), entry.getValue());
			}
		}

		if (this.currentProject.getCurrentMachine() != null) {
			this.currentProject.reloadCurrentMachine();
		}
	}
	
	public void rollback() {
		this.globalProBPrefs.rollback();
	}
	
	public void restoreDefaults() {
		for (ProBPreference pref : this.globalProBPrefs.getPreferences().values()) {
			this.globalProBPrefs.setPreferenceValue(pref.name, pref.defaultValue);
		}
		this.globalPrefsView.refresh();
		this.injector.getInstance(DotView.class).refresh();
		this.injector.getInstance(ExpressionTableView.class).refresh();
	}
	
	public void undo() {
		this.globalProBPrefs.rollback();
		this.globalPrefsView.refresh();
		this.injector.getInstance(DotView.class).refresh();
		this.injector.getInstance(ExpressionTableView.class).refresh();
	}
	
}
