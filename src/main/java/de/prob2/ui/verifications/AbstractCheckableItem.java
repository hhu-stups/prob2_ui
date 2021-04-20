package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractCheckableItem implements IExecutableItem {
	private final String name;
	private final String description;
	private final String code;
	private BooleanProperty selected;
	@JsonIgnore
	final ObjectProperty<CheckingResultItem> resultItem = new SimpleObjectProperty<>(this, "resultItem", null);
	@JsonIgnore
	final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
	
	public AbstractCheckableItem(String name, String description, String code) {
		this.name = name;
		this.description = description;
		this.code = code;
		this.selected = new SimpleBooleanProperty(true);
		
		this.initListeners();
	}
	
	private void initListeners() {
		this.resultItemProperty().addListener((o, from, to) -> this.checked.set(to == null ? Checked.NOT_CHECKED : to.getChecked()));
	}
	
	public void reset() {
		this.setResultItem(null);
	}
	
	public String getName() {
		return name;
	}
	
	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}
	
	@JsonProperty("selected")
	@Override
	public boolean selected() {
		return selected.get();
	}
	
	public BooleanProperty selectedProperty() {
		return selected;
	}

	public String getDescription() {
		return description;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setResultItem(CheckingResultItem resultItem) {
		this.resultItem.set(resultItem);
	}
	
	public CheckingResultItem getResultItem() {
		return resultItem.get();
	}
	
	public ObjectProperty<CheckingResultItem> resultItemProperty() {
		return resultItem;
	}
	
	@Override
	public ReadOnlyObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}
	
	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}
}
