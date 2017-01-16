package de.prob2.ui.prob2fx;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractModel;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

/**
 * A singleton read-only property representing the current {@link AbstractModel}. Currently this is just a normal property, but convenience properties and methods like those in {@link CurrentTrace} may be added later.
 */
@Singleton
public final class CurrentModel extends ReadOnlyObjectProperty<AbstractModel> {
	private final ObjectProperty<AbstractModel> model;
	
	@Inject
	private CurrentModel() {
		super();
		
		this.model = new SimpleObjectProperty<>(this, "model", null);
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
		return this.model.get();
	}
	
	@Override
	public void addListener(final ChangeListener<? super AbstractModel> listener) {
		this.model.addListener(listener);
	}
	
	@Override
	public void removeListener(final ChangeListener<? super AbstractModel> listener) {
		this.model.removeListener(listener);
	}
	
	@Override
	public void addListener(final InvalidationListener listener) {
		this.model.addListener(listener);
	}
	
	@Override
	public void removeListener(final InvalidationListener listener) {
		this.model.removeListener(listener);
	}
	
	/* package */ void set(final AbstractModel stateSpace) {
		this.model.set(stateSpace);
	}
}
