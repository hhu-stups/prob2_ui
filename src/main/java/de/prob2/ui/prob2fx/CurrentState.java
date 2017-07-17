package de.prob2.ui.prob2fx;

import java.util.function.BooleanSupplier;

import de.prob.statespace.State;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyObjectPropertyBase;

/**
 * A singleton read-only property representing the current {@link State}. It also provides convenience properties and methods for easy interaction with JavaFX components using property binding.
 */
public final class CurrentState extends ReadOnlyObjectPropertyBase<State> {
	private final class ROBoolProp extends ReadOnlyBooleanPropertyBase {
		private final String name;
		private final BooleanSupplier getter;
		
		private ROBoolProp(final String name, final BooleanSupplier getter) {
			super();
			
			this.name = name;
			this.getter = getter;
			
			CurrentState.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public boolean get() {
			return this.getter.getAsBoolean();
		}
		
		@Override
		public Object getBean() {
			return CurrentState.this;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
	}
	
	private final CurrentTrace currentTrace;
	private final ReadOnlyBooleanProperty initialized;
	
	CurrentState(final CurrentTrace currentTrace) {
		super();
		
		this.currentTrace = currentTrace;
		currentTrace.addListener(o -> this.fireValueChangedEvent());
		this.initialized = new ROBoolProp("initialized", () -> this.get() != null && this.get().isInitialised());
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
		return this.currentTrace.exists() ? this.currentTrace.get().getCurrentState() : null;
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
