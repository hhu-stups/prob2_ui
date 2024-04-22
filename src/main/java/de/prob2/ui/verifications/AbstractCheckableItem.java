package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractCheckableItem implements IExecutableItem {
	private final BooleanProperty selected;
	@JsonIgnore
	final ObjectProperty<CheckingResultItem> resultItem = new SimpleObjectProperty<>(this, "resultItem", null);
	@JsonIgnore
	final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	protected AbstractCheckableItem() {
		this.selected = new SimpleBooleanProperty(true);

		this.resultItemProperty().addListener((o, from, to) -> this.checked.set(to == null ? Checked.NOT_CHECKED : to.getChecked()));
	}

	@Override
	@JsonSetter("selected")
	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}

	@Override
	@JsonGetter("selected")
	public boolean selected() {
		return this.selected.get();
	}

	@Override
	public BooleanProperty selectedProperty() {
		return selected;
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

	@Override
	public void reset() {
		this.setResultItem(null);
	}
}
