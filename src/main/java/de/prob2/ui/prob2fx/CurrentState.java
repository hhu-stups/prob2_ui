package de.prob2.ui.prob2fx;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.State;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

/**
 * A singleton read-only property representing the current {@link State}. It also provides convenience properties and methods for easy interaction with JavaFX components using property binding.
 */
@Singleton
public final class CurrentState extends ReadOnlyObjectProperty<State> {
	private final ObjectProperty<State> state;
	private final BooleanProperty initialized;
	
	@Inject
	private CurrentState() {
		super();
		
		this.state = new SimpleObjectProperty<>(this, "state", null);
		this.initialized = new SimpleBooleanProperty(this, "initialized", false);
		
		this.addListener((observable, from, to) -> {
			if (to == null) {
				this.initialized.set(false);
			} else {
				this.initialized.set(to.isInitialised());
			}
		});
	}
	
	@Override
	public Object getBean() {
		return null;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public State get() {
		return this.state.get();
	}
	
	@Override
	public void addListener(final ChangeListener<? super State> listener) {
		this.state.addListener(listener);
	}
	
	@Override
	public void removeListener(final ChangeListener<? super State> listener) {
		this.state.removeListener(listener);
	}
	
	@Override
	public void addListener(final InvalidationListener listener) {
		this.state.addListener(listener);
	}
	
	@Override
	public void removeListener(final InvalidationListener listener) {
		this.state.removeListener(listener);
	}
	
	/* package */ void set(final State stateSpace) {
		this.state.set(stateSpace);
	}
	
	/**
	 * A read-only boolean property indicating whether the current state is initialized.
	 * 
	 * @return a read-only boolean property indicating whether the current state is initialized.
	 */
	public ReadOnlyBooleanProperty initializedProperty() {
		return this.initialized;
	}
	
	/**
	 * Return whether the current state is initialized.
	 * 
	 * @return whether the current state is initialized
	 */
	public boolean isInitialized() {
		return this.initializedProperty().get();
	}
}
