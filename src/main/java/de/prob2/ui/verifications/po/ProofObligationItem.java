package de.prob2.ui.verifications.po;

import de.prob.model.eventb.ProofObligation;
import de.prob.util.Tuple2;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.vomanager.IValidationTask;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ProofObligationItem extends AbstractCheckableItem implements IValidationTask {

	private String id;
	private final String name;
	private final String description;
	private final String sourceName;
	private final boolean discharged;

	public ProofObligationItem(final String name, final String description, final String sourceName, boolean discharged, List<Tuple2<String, String>> content) {
		this.id = "";
		this.name = name;
		this.description = description;
		this.sourceName = sourceName;
		this.discharged = discharged;
		this.setResultItem(new CheckingResultItem(discharged ? Checked.SUCCESS : Checked.UNKNOWN, ""));
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this.id = "";
		this.name = proofObligation.getName();
		this.description = proofObligation.getDescription();
		this.sourceName = proofObligation.getSourceName();
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

	public String getSourceName() {
		return sourceName;
	}

	public boolean isDischarged() {
		return discharged;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ProofObligationItem.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("name='" + name + "'")
				.add("description='" + description + "'")
				.add("sourceName='" + sourceName + "'")
				.add("discharged=" + discharged)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProofObligationItem that = (ProofObligationItem) o;
		return id.equals(that.id) && discharged == that.discharged && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(sourceName, that.sourceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, sourceName, discharged);
	}
}
