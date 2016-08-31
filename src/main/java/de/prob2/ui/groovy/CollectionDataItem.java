package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;

public class CollectionDataItem {
	
	private final SimpleStringProperty index;
	private final SimpleStringProperty value;
	
	public CollectionDataItem(int index, Object value) {
		this.index = new SimpleStringProperty(new Integer(index).toString());
		this.value = new SimpleStringProperty(value.toString());
	}
	
	public String getIndex() {
		return index.get();
	}
	
	public void setIndex(int index) {
		this.index.set(new Integer(index).toString());
	}
	
	public String getValue() {
		return value.get();
	}
	
	public void setValue(Object value) {
		this.value.set(value.toString());
	}
	

}
