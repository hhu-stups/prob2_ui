package de.prob2.ui.preferences;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import de.prob.animator.domainobjects.ProBPreference;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * The state behind a {@link PreferencesView}.
 * Stores a collection of {@link ProBPreference}s that should be shown in the view,
 * the current values of these preferences,
 * and the changed values entered by the user.
 */
public final class ProBPreferences {
	private final Map<String, ProBPreference> preferenceInfos;
	private final Map<String, String> defaultPreferenceValues;
	private final ObservableMap<String, String> currentPreferenceValues;
	private final ObservableMap<String, String> preferenceChanges;
	private final BooleanProperty changesApplied;
	
	/**
	 * Create a new preferences state.
	 * 
	 * @param preferenceInfos the preferences to show in the view
	 */
	public ProBPreferences(final Collection<ProBPreference> preferenceInfos) {
		this.preferenceInfos = new HashMap<>();
		for (final ProBPreference pref : preferenceInfos) {
			this.preferenceInfos.put(pref.name, pref);
		}
		final Map<String, String> defaultValues = new HashMap<>();
		this.getPreferenceInfos().forEach((k, v) -> defaultValues.put(k, v.defaultValue));
		this.defaultPreferenceValues = Collections.unmodifiableMap(defaultValues);
		this.currentPreferenceValues = FXCollections.observableHashMap();
		this.currentPreferenceValues.putAll(this.getDefaultPreferenceValues());
		this.preferenceChanges = FXCollections.observableHashMap();
		this.changesApplied = new SimpleBooleanProperty(this, "changesApplied", true);
		this.changesApplied.bind(Bindings.createBooleanBinding(this.preferenceChanges::isEmpty, this.preferenceChanges));
	}
	
	/**
	 * A property indicating whether all changes have been applied.
	 * 
	 * @return a property indicating whether all changes have been applied
	 */
	public ReadOnlyBooleanProperty changesAppliedProperty() {
		return this.changesApplied;
	}
	
	/**
	 * Get whether all changes have been applied.
	 * 
	 * @return whether all changes have been applied
	 */
	public boolean getChangesApplied() {
		return this.changesApplied.get();
	}
	
	/**
	 * Get information about all preferences tracked by this object.
	 * The returned {@link ProBPreference} objects do not include the current values or pending changes for the preferences.
	 * To get the current preference values, use {@link #getCurrentPreferenceValues()}.
	 * To get 
	 * {@link #getPreferenceValueWithChanges(String)} or {@link #getPreferenceValuesWithChanges()}.
	 * 
	 * @return information about all preferences tracked by this object
	 */
	public Map<String, ProBPreference> getPreferenceInfos() {
		return Collections.unmodifiableMap(this.preferenceInfos);
	}
	
	/**
	 * Get the default values of all tracked preferences.
	 * This information is taken from the {@link ProBPreference} objects and doesn't change.
	 * 
	 * @return map with default values of all tracked preferences
	 */
	public Map<String, String> getDefaultPreferenceValues() {
		return this.defaultPreferenceValues;
	}
	
	/**
	 * Get a read-only observable map with the current values of all tracked preferences.
	 * This doesn't include any unapplied preference changes -
	 * to inspect these changes,
	 * use {@link #getPreferenceChanges()} or {@link #getPreferenceValuesWithChanges()}.
	 * 
	 * @return read-only observable map with current values of all tracked preferences
	 */
	public ObservableMap<String, String> getCurrentPreferenceValues() {
		return FXCollections.unmodifiableObservableMap(this.currentPreferenceValues);
	}
	
	/**
	 * <p>
	 * Update the current values of some or all tracked preferences.
	 * Preferences in {@code preferenceValues} that are not tracked by this object are ignored.
	 * Preferences tracked by this object that don't appear in {@code preferenceValues} remain unchanged.
	 * Calling this method discards all pending preference changes and directly updates the current preference values.
	 * </p>
	 * <p>
	 * This method should be used to notify this object of preference changes that happened externally,
	 * for example when another part of the UI changes some preferences.
	 * It should not be used for changes performed by the user inside the {@link PreferencesView} -
	 * such changes should be performed using {@link #changePreference(String, String)}.
	 * </p>
	 * 
	 * @param preferenceValues the preferences to update and their values
	 */
	public void updateCurrentPreferenceValues(final Map<String, String> preferenceValues) {
		final Map<String, String> applicableValues = new HashMap<>(preferenceValues);
		applicableValues.keySet().retainAll(this.currentPreferenceValues.keySet());
		this.preferenceChanges.clear();
		this.currentPreferenceValues.putAll(applicableValues);
	}
	
	/**
	 * Similar to {@link #updateCurrentPreferenceValues(Map)},
	 * except that all preferences that don't appear in {@code preferenceValues} are reset to their default values.
	 * 
	 * @param preferenceValues the preferences and their values which should not be reset to defaults
	 */
	public void setCurrentPreferenceValues(final Map<String, String> preferenceValues) {
		final Map<String, String> valuesWithDefaults = new HashMap<>(this.getDefaultPreferenceValues());
		valuesWithDefaults.putAll(preferenceValues);
		this.updateCurrentPreferenceValues(valuesWithDefaults);
	}
	
	/**
	 * Get a read-only observable map containing all pending preference changes entered by the user.
	 * Preferences that the user has not changed (compared to {@link #getCurrentPreferenceValues()}) do not appear in this map.
	 * 
	 * @return a read-only observable map containing all pending preference changes
	 */
	public ObservableMap<String, String> getPreferenceChanges() {
		return FXCollections.unmodifiableObservableMap(this.preferenceChanges);
	}
	
	/**
	 * <p>
	 * Prepare changing the value of the given preference as a result of user input.
	 * This only changes the {@link #getPreferenceChanges()} map,
	 * until {@link #apply()} is used to apply all pending preference changes.
	 * </p>
	 * <p>
	 * This method should only be used for preference changes performed by the user in the {@link PreferencesView} corresponding to this object.
	 * Preference changes caused by other parts of the UI should be applied using {@link #updateCurrentPreferenceValues(Map)} or {@link #setCurrentPreferenceValues(Map)}.
	 * </p>
	 * 
	 * @param name the preference to change
	 * @param value the value to change the preference to
	 */
	public void changePreference(String name, String value) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(value);
		
		if (!this.currentPreferenceValues.containsKey(name)) {
			throw new NoSuchElementException("Unknown preference name: " + name);
		}
		
		this.preferenceChanges.put(name, value);
		if (value.equals(this.currentPreferenceValues.get(name))) {
			this.preferenceChanges.remove(name);
		}
	}
	
	/**
	 * Get the value of the given preference,
	 * including any pending change to its value.
	 * 
	 * @param name the preference to get the value for
	 * @return the preference's value including changes
	 * 
	 * @see #getPreferenceValuesWithChanges()
	 */
	public String getPreferenceValueWithChanges(final String name) {
		Objects.requireNonNull(name);
		
		return this.preferenceChanges.containsKey(name) ? this.preferenceChanges.get(name) : this.currentPreferenceValues.get(name);
	}
	
	/**
	 * Get the values of all preferences,
	 * including any pending preference changes.
	 * 
	 * @return the values of all preferences including changes
	 * 
	 * @see #getPreferenceValueWithChanges(String)
	 */
	public Map<String, String> getPreferenceValuesWithChanges() {
		final Map<String, String> prefs = new HashMap<>(this.currentPreferenceValues);
		prefs.putAll(this.preferenceChanges);
		return Collections.unmodifiableMap(prefs);
	}
	
	/**
	 * Apply all pending preference changes.
	 * This clears {@link #getPreferenceChanges()} and merges its contents into {@link #getCurrentPreferenceValues()}.
	 * 
	 * @see #rollback()
	 */
	public void apply() {
		this.updateCurrentPreferenceValues(this.getPreferenceChanges());
	}
	
	/**
	 * Rollback all preference changes made since the last {@link #apply()} call.
	 * This clears {@link #getPreferenceChanges()} and undoes all changes.
	 * {@link #getCurrentPreferenceValues()} is not changed.
	 * 
	 * @see #apply()
	 */
	public void rollback() {
		this.preferenceChanges.clear();
	}
	
	/**
	 * Prepare changing the values of all tracked preferences back to their default values.
	 * This is different from {@link #rollback()},
	 * which discards all pending changes performed by the user,
	 * but doesn't necessarily restore the default values.
	 */
	public void restoreDefaults() {
		this.getPreferenceInfos().values().forEach(pref -> this.changePreference(pref.name, pref.defaultValue));
	}
}
