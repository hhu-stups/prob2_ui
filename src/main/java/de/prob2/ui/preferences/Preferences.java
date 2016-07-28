package de.prob2.ui.preferences;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Preferences {
	private final AnimationSelector animationSelector;
	private final Api api;
	private final EventBus eventBus;
	private final BooleanProperty changesApplied;
	private StateSpace stateSpace;
	
	@Inject
	private Preferences(
		final AnimationSelector animationSelector,
		final Api api,
		final EventBus eventBus
	) {
		this.animationSelector = animationSelector;
		this.api = api;
		this.eventBus = eventBus;
		this.changesApplied = new SimpleBooleanProperty(true);
		this.stateSpace = null;
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
	 * Ensure that {@link #stateSpace} is not {@link null}.
	 * 
	 * @throws IllegalStateException if {@link #stateSpace} is {@link null}
	 */
	private void checkStateSpace() {
		if (this.stateSpace == null) {
			throw new IllegalStateException("Cannot use Preferences without setting a StateSpace first");
		}
	}
	
	/**
	 * Set a {@link StateSpace} to be used by this instance.
	 * This method must be called before any of the other methods are used and will throw an {@link IllegalStateException} otherwise.
	 * 
	 * @param stateSpace the {@link StateSpace} to use
	 */
	public void setStateSpace(StateSpace stateSpace) {
		this.stateSpace = stateSpace;
		this.changesApplied.set(true);
	}
	
	/**
	 * Get information about all available preferences.
	 * The returned {@link ProBPreference} objects do not include the current values of the preferences. To get these values, use {@link #getPreferenceValue(String)} or {@link #getPreferenceValues()}.
	 * 
	 * @return information about all available preferences
	 * @see #getPreferenceValue(String)
	 * @see #getPreferenceValues()
	 */
	public List<ProBPreference> getPreferences() {
		this.checkStateSpace();
		GetDefaultPreferencesCommand cmd = new GetDefaultPreferencesCommand();
		this.stateSpace.execute(cmd);
		return cmd.getPreferences();
	}
	
	/**
	 * Get the current value of the given preference.
	 * 
	 * @param name the preference to get the value for
	 * @return the preference's current value
	 * @see #getPreferenceValues()
	 */
	public String getPreferenceValue(String name) {
		this.checkStateSpace();
		GetPreferenceCommand cmd = new GetPreferenceCommand(name);
		this.stateSpace.execute(cmd);
		return cmd.getValue();
	}
	
	/**
	 * Get the current values of all preferences.
	 * 
	 * @return the current values of all preferences
	 * @see #getPreferenceValue(String)
	 * @see #getPreferences()
	 */
	public Map<String, String> getPreferenceValues() {
		this.checkStateSpace();
		GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
		this.stateSpace.execute(cmd);
		return cmd.getPreferences();
	}
	
	/**
	 * Set the value of the given preference.
	 * Note that for some preferences to take effect, the current model needs to be reloaded using {@link #apply()}.
	 * 
	 * @param name the preference to set
	 * @param value the value to set the preference to
	 * @see #apply()
	 */
	public void setPreferenceValue(String name, String value) {
		this.checkStateSpace();
		this.stateSpace.execute(new SetPreferenceCommand(name, value));
		this.changesApplied.set(false);
	}
	
	/**
	 * Reload the current model and apply all preference changes.
	 *
	 * @throws IllegalStateException if there is no current trace
	 * @see #setPreferenceValue(String, String)
	 */
	public void apply() {
		Map<String, String> savedPrefs = this.getPreferenceValues();
		Trace oldTrace = this.animationSelector.getCurrentTrace();
		if (oldTrace == null) {
			throw new IllegalStateException("Cannot apply preferences without a current trace");
		}
		String filename = oldTrace.getModel().getModelFile().getAbsolutePath();
		StateSpace newSpace;
		try {
			newSpace = api.b_load(filename, savedPrefs);
		} catch (IOException | BException e) {
			this.eventBus.post(e);
			return;
		}
		Trace newTrace = new Trace(newSpace);
		this.animationSelector.addNewAnimation(newTrace);
		this.animationSelector.removeTrace(oldTrace);
		this.changesApplied.set(true);
	}
}
