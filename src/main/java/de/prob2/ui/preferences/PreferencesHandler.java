package de.prob2.ui.preferences;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesHandler.class);
	
	private final StageManager stageManager;
	
	private final ObjectProperty<ProBPreferences> preferences;
	
	@Inject
	private PreferencesHandler(final StageManager stageManager) {
		this.stageManager = stageManager;
		
		this.preferences = new SimpleObjectProperty<>(this, "preferences");
	}

	public ObjectProperty<ProBPreferences> preferencesProperty() {
		return this.preferences;
	}
	
	public ProBPreferences getPreferences() {
		return this.preferencesProperty().get();
	}
	
	public void setPreferences(final ProBPreferences preferences) {
		this.preferencesProperty().set(preferences);
	}

	public void apply() {
		try {
			this.getPreferences().apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content").show();
		}
	}
	
	public void rollback() {
		this.getPreferences().rollback();
	}
	
	public void restoreDefaults() {
		for (ProBPreference pref : this.getPreferences().getPreferences().values()) {
			this.getPreferences().setPreferenceValue(pref.name, pref.defaultValue);
		}
	}
}
