package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"selected",
})
public abstract class SymbolicAnimationItem extends AbstractCheckableItem implements IValidationTask {
	@JsonIgnore
	private final ObjectProperty<Trace> example = new SimpleObjectProperty<>(this, "example", null);

	protected SymbolicAnimationItem() {
		super();
	}

	@JsonIgnore // TODO
	@Override
	public String getId() {
		return null; // TODO
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof SymbolicAnimationItem o
			&& this.getTaskType().equals(o.getTaskType())
			&& Objects.equals(this.getId(), o.getId());
	}

	@Override
	public void reset() {
		super.reset();
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

	@Override
	public void execute(final ExecutionContext context) {
		SymbolicAnimationItemHandler.executeItem(this, context.stateSpace());
	}
}
