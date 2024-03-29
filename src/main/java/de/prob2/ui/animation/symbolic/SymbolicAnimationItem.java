package de.prob2.ui.animation.symbolic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"type",
	"code",
	"selected",
})
public class SymbolicAnimationItem extends AbstractCheckableItem {
	private final SymbolicAnimationType type;
	private final String code;

	@JsonIgnore
	private final ObjectProperty<Trace> example = new SimpleObjectProperty<>(this, "example", null);

	@JsonCreator
	public SymbolicAnimationItem(
		@JsonProperty("code") final String code,
		@JsonProperty("type") final SymbolicAnimationType type
	) {
		super();
		this.type = type;
		this.code = code;
	}

	public SymbolicAnimationType getType() {
		return this.type;
	}

	public String getCode() {
		return this.code;
	}

	@Override
	public boolean settingsEqual(Object other) {
		if (!(other instanceof SymbolicAnimationItem that)) {
			return false;
		}
		return this.getType().equals(that.getType())
			       && this.getCode().equals(that.getCode());
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
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("type", this.getType())
			       .add("code", this.getCode())
			       .toString();
	}

	@Override
	public void execute(final ExecutionContext context) {
		SymbolicAnimationItemHandler.executeItem(this, context.stateSpace());
	}
}
