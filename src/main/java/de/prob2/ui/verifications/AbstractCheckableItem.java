package de.prob2.ui.verifications;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.MoreObjects;

import de.prob.statespace.Trace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"selected",
})
public abstract class AbstractCheckableItem implements ISelectableTask {
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String id;
	private final BooleanProperty selected;

	@JsonIgnore
	final ObjectProperty<ICheckingResult> result = new SimpleObjectProperty<>(this, "result", null);
	@JsonIgnore
	final ObjectProperty<CheckingStatus> status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);

	protected AbstractCheckableItem(String id) {
		this.id = id != null && !id.isEmpty() ? id : null; // may be null for tasks that have no ID!
		this.selected = new SimpleBooleanProperty(true);

		this.resultProperty().addListener((o, from, to) -> this.status.set(to == null ? CheckingStatus.NOT_CHECKED : to.getStatus()));
	}

	@Override
	public String getId() {
		return this.id;
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

	public void setResult(ICheckingResult result) {
		this.result.set(result);
	}

	public ICheckingResult getResult() {
		return result.get();
	}

	public ObjectProperty<ICheckingResult> resultProperty() {
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

	@JsonIgnore
	public Trace getTrace() {
		return this.getResult() != null ? this.getResult().getTrace() : null;
	}

	@Override
	public void resetAnimatorDependentState() {
		if (this.getResult() != null) {
			this.setResult(this.getResult().withoutAnimatorDependentState());
		}
	}

	@Override
	public void reset() {
		this.setResult(null);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof AbstractCheckableItem o
			&& this.getTaskType().equals(o.getTaskType())
			&& Objects.equals(this.getId(), o.getId());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.omitNullValues()
			.add("id", this.getId())
			.toString();
	}
}
