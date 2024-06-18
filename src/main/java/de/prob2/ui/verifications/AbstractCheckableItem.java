package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractCheckableItem implements ISelectableTask {
	private final BooleanProperty selected;
	@JsonIgnore
	final ObjectProperty<CheckingResult> result = new SimpleObjectProperty<>(this, "result", null);
	@JsonIgnore
	final ObjectProperty<CheckingStatus> status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);

	protected AbstractCheckableItem() {
		this.selected = new SimpleBooleanProperty(true);

		this.resultProperty().addListener((o, from, to) -> this.status.set(to == null ? CheckingStatus.NOT_CHECKED : to.getStatus()));
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

	public void setResult(CheckingResult result) {
		this.result.set(result);
	}

	public CheckingResult getResult() {
		return result.get();
	}

	public ObjectProperty<CheckingResult> resultProperty() {
		return result;
	}

	@Override
	public ReadOnlyObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	@Override
	public void reset() {
		this.setResult(null);
		this.resetAnimatorDependentState();
	}
}
