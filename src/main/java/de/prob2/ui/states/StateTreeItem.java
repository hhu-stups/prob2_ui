package de.prob2.ui.states;

import java.util.Map;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.Map;

public abstract class StateTreeItem<T> {
	protected StringProperty name;
	public ReadOnlyStringProperty nameProperty() {
		return this.name;
	}
	public String getName() {
		return this.name.get();
	}
	
	protected StringProperty value;
	public ReadOnlyStringProperty valueProperty() {
		return this.value;
	}
	public String getValue() {
		return this.value.get();
	}
	
	protected StringProperty previousValue;
	public ReadOnlyStringProperty previousValueProperty() {
		return this.previousValue;
	}
	public String getPreviousValue() {
		return this.previousValue.get();
	}
	
	protected T contents;
	public T getContents() {
		return this.contents;
	}
	
	public StateTreeItem(final String name, final String value, final String previousValue, final T contents) {
		super();
		this.name = new SimpleStringProperty(name);
		this.value = new SimpleStringProperty(value);
		this.previousValue = new SimpleStringProperty(previousValue);
		this.contents = contents;
	}
	
	public StateTreeItem(final String name, final String value, final String previousValue) {
		this(name, value, previousValue, null);
	}
	
	public StateTreeItem(final T contents) {
		this("", "", "", contents);
	}
	
	public StateTreeItem() {
		this("", "", "");
	}
	
	public void update(
		final Map<IEvalElement, AbstractEvalResult> values,
		final Map<IEvalElement, AbstractEvalResult> previousValues
	) {}
}
