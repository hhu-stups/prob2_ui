package de.prob2.ui.states;

import de.prob.statespace.Trace;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class StateTreeItem<T> implements Comparable<StateTreeItem<?>> {
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
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof StateTreeItem)) {
			return false;
		}
		
		StateTreeItem sti = (StateTreeItem)obj;
		return this.getContents().equals(sti.getContents());
	}
	
	@Override
	public int compareTo(StateTreeItem<?> o) {
		return this.getName().compareTo(o.getName());
	}
	
	public void update(final Trace trace) {}
}
