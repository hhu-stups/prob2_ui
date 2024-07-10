package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;

@JsonPropertyOrder({
	"id",
	"selected",
})
public abstract class SymbolicAnimationItem extends AbstractCheckableItem implements ICliTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

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
}
