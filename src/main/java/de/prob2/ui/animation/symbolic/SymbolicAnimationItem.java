package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"selected",
})
public abstract class SymbolicAnimationItem extends AbstractCheckableItem implements ICliTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	@JsonIgnore
	private final ObjectProperty<Trace> example = new SimpleObjectProperty<>(this, "example", null);

	protected SymbolicAnimationItem(String id) {
		super();
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof SymbolicAnimationItem o
			&& this.getTaskType().equals(o.getTaskType())
			&& Objects.equals(this.getId(), o.getId());
	}

	@Override
	public void resetAnimatorDependentState() {
		this.setExample(null);
	}

	public ObjectProperty<Trace> exampleProperty() {
		return this.example;
	}

	public Trace getExample() {
		return this.exampleProperty().get();
	}

	public void setExample(final Trace example) {
		this.exampleProperty().set(example);
	}
}
