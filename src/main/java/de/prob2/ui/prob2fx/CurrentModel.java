package de.prob2.ui.prob2fx;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;

import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;

/**
 * A singleton read-only property representing the current {@link AbstractModel}. It also provides convenience properties and methods for easy interaction with JavaFX components using property binding.
 */
public final class CurrentModel extends ReadOnlyObjectPropertyBase<AbstractModel> {
	private final class ROBoolProp extends ReadOnlyBooleanPropertyBase {
		private final String name;
		private final BooleanSupplier getter;
		
		private ROBoolProp(final String name, final BooleanSupplier getter) {
			super();
			
			this.name = name;
			this.getter = getter;
			
			CurrentModel.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public boolean get() {
			return this.getter.getAsBoolean();
		}
		
		@Override
		public Object getBean() {
			return CurrentModel.this;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
	}
	
	private final class ROObjProp<T> extends ReadOnlyObjectPropertyBase<T> {
		private final String name;
		private final Function<AbstractModel, T> getter;
		private final T noTraceDefault;
		
		private ROObjProp(final String name, final Function<AbstractModel, T> getter, final T noTraceDefault) {
			super();
			
			this.name = name;
			this.getter = getter;
			this.noTraceDefault = noTraceDefault;
			
			CurrentModel.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public T get() {
			final AbstractModel model = CurrentModel.this.get();
			return model == null ? this.noTraceDefault : this.getter.apply(model);
		}
		
		@Override
		public Object getBean() {
			return CurrentModel.this;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
	}
	
	private final CurrentTrace currentTrace;
	
	private final ReadOnlyObjectProperty<FormalismType> formalismType;
	
	CurrentModel(final CurrentTrace currentTrace) {
		super();
		
		this.currentTrace = currentTrace;
		currentTrace.addListener(o -> this.fireValueChangedEvent());
		
		this.formalismType = new ROObjProp<>("formalismType", AbstractModel::getFormalismType, null);
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
	public AbstractModel get() {
		final Trace trace = this.currentTrace.get();
		return trace != null ? trace.getModel() : null;
	}
	
	public ReadOnlyObjectProperty<FormalismType> formalismTypeProperty() {
		return this.formalismType;
	}
	
	public FormalismType getFormalismType() {
		return this.formalismTypeProperty().get();
	}
}
