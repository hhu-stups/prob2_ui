package de.prob2.ui.verifications.modelchecking;


import com.google.inject.Singleton;

import de.prob.check.IModelCheckJob;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public final class CurrentJob extends SimpleObjectProperty<IModelCheckJob> {
	
	private final SimpleBooleanProperty exists;
	
	public CurrentJob() {
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
	}
	
	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	public boolean exists() {
		return this.existsProperty().get();
	}
	
}
