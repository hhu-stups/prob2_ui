package de.prob2.ui.verifications.po;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.prob.model.eventb.ProofObligation;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class ProofObligationItem implements IValidationTask {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String name;
	private final String description;
	private final ObjectProperty<Checked> checked;

	public ProofObligationItem(final String id, final String name, final String description) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.PARSE_ERROR);
	}

	public ProofObligationItem(final SavedProofObligationItem po) {
		this(po.getId(), po.getName(), "");
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this(null, proofObligation.getName(), proofObligation.getDescription());
		this.setChecked(proofObligation.isDischarged() ? Checked.SUCCESS : Checked.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ValidationTaskType getTaskType() {
		return BuiltinValidationTaskTypes.PROOF_OBLIGATION;
	}

	public ProofObligationItem withId(final String id) {
		final ProofObligationItem updatedPO = new ProofObligationItem(id, this.getName(), this.getDescription());
		updatedPO.setChecked(this.getChecked());
		return updatedPO;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("verifications.po.type");
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

	public String getDescription() {
		return description;
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}

	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	public void setChecked(final Checked checked) {
		this.checkedProperty().set(checked);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ProofObligationItem.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("name='" + name + "'")
				.add("description='" + description + "'")
				.add("checked=" + getChecked())
				.toString();
	}

	public boolean settingsEqual(final ProofObligationItem that) {
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description);
	}
}
