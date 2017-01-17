package de.prob2.ui.prob2fx;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.StateSpace;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

/**
 * A singleton read-only property representing the current {@link StateSpace}. Currently this is just a normal property, but convenience properties and methods like those in {@link CurrentTrace} may be added later.
 */
@Singleton
public final class CurrentStateSpace extends ReadOnlyObjectProperty<StateSpace> {
	private final ObjectProperty<StateSpace> stateSpace;
	
	@Inject
	private CurrentStateSpace() {
		super();
		
		this.stateSpace = new SimpleObjectProperty<>(this, "stateSpace", null);
	}
	
	@Override
	public Object getBean() {
		return null;
	}
	
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public StateSpace get() {
		return this.stateSpace.get();
	}
	
	@Override
	public void addListener(final ChangeListener<? super StateSpace> listener) {
		this.stateSpace.addListener(listener);
	}
	
	@Override
	public void removeListener(final ChangeListener<? super StateSpace> listener) {
		this.stateSpace.removeListener(listener);
	}
	
	@Override
	public void addListener(final InvalidationListener listener) {
		this.stateSpace.addListener(listener);
	}
	
	@Override
	public void removeListener(final InvalidationListener listener) {
		this.stateSpace.removeListener(listener);
	}
	
	/* package */ void set(final StateSpace stateSpace) {
		this.stateSpace.set(stateSpace);
	}
}
