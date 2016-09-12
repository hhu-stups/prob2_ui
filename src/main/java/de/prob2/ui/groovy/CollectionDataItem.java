package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;

public class CollectionDataItem {
	
	private final SimpleStringProperty index;
	private final SimpleStringProperty value;
	
	public CollectionDataItem(int index, Object value) {
		this.index = new SimpleStringProperty(Integer.toString(index));
		this.value = new SimpleStringProperty(value.toString());
	}
	
	public String getIndex() {
		return index.get();
	}
	
	public void setIndex(int index) {
		this.index.set(Integer.toString(index));
	}
	
	public String getValue() {
		return value.get();
	}
	
	public void setValue(Object value) {
		this.value.set(value.toString());
	}
	

}
