package de.prob2.ui.verifications.po;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.prob.model.eventb.ProofObligation;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.vomanager.IValidationTask;

import java.util.Objects;
import java.util.StringJoiner;

@JsonPropertyOrder({
		"id",
		"name",
		"description",
})
public class ProofObligationItem extends AbstractCheckableItem implements IValidationTask {

	private String id;
	private final String name;
	private final String description;

	@JsonIgnore
	private boolean discharged;

	@JsonCreator
	public ProofObligationItem(
			@JsonProperty("id") final String id,
			@JsonProperty("name") final String name,
			@JsonProperty("description") final String description
	) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public ProofObligationItem(final String name, final String description, final String sourceName, boolean discharged) {
		this.id = null;
		this.name = name;
		this.description = description;
		this.discharged = discharged;
		this.setResultItem(new CheckingResultItem(discharged ? Checked.SUCCESS : Checked.UNKNOWN, ""));
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this.id = null;
		this.name = proofObligation.getName();
		this.description = proofObligation.getDescription();
		this.discharged = proofObligation.isDischarged();
		this.setResultItem(new CheckingResultItem(discharged ? Checked.SUCCESS : Checked.UNKNOWN, ""));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		if (this.getDescription().isEmpty()) {
			return this.getName();
		} else {
			return this.getName() + " // " + getDescription();
		}
	}

	public String getName() {
		return name;
	}

	public boolean isDischarged() {
		return discharged;
	}

	public void setDischarged(boolean discharged) {
		this.discharged = discharged;
		this.setResultItem(new CheckingResultItem(discharged ? Checked.SUCCESS : Checked.UNKNOWN, ""));
	}

	public String getDescription() {
		return description;
	}

	@JsonIgnore
	@Override
	public String toString() {
		return new StringJoiner(", ", ProofObligationItem.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("name='" + name + "'")
				.add("description='" + description + "'")
				.add("discharged=" + discharged)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProofObligationItem that = (ProofObligationItem) o;
		return Objects.equals(id, that.id) && discharged == that.discharged && Objects.equals(name, that.name) && Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, discharged);
	}
}
