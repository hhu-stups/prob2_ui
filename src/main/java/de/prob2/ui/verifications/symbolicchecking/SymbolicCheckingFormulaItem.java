package de.prob2.ui.verifications.symbolicchecking;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;

@JsonPropertyOrder({
	"id",
	"selected",
})
public abstract class SymbolicCheckingFormulaItem extends AbstractCheckableItem implements ICliTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	protected SymbolicCheckingFormulaItem(String id) {
		super();
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof SymbolicCheckingFormulaItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .toString();
	}
}
