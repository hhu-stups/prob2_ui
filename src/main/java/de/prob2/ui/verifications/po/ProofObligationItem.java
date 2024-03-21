package de.prob2.ui.verifications.po;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import de.prob.model.eventb.ProofObligation;
import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class ProofObligationItem implements IValidationTask<ProofObligationItem> {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String name;
	@JsonIgnore
	private final String description;
	@JsonIgnore
	private final ObjectProperty<Checked> checked;

	public ProofObligationItem(String id, String name, String description) {
		this.id = id;
		this.name = Objects.requireNonNull(name, "name");
		this.description = Objects.requireNonNull(description, "description");
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.INVALID_TASK);
	}

	@JsonCreator
	public ProofObligationItem(@JsonProperty("id") String id, @JsonProperty("name") String name) {
		this(id, name, "");
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this(null, proofObligation.getName(), Objects.requireNonNullElse(proofObligation.getDescription(), ""));
		this.setChecked(proofObligation.isDischarged() ? Checked.SUCCESS : Checked.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ValidationTaskType<ProofObligationItem> getTaskType() {
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
		return this.name;
	}

	@JsonIgnore
	public String getDescription() {
		return this.description;
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}

	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	@JsonIgnore
	public void setChecked(final Checked checked) {
		this.checkedProperty().set(checked);
	}

	@Override
	public void reset() {
		this.setChecked(Checked.NOT_CHECKED);
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ProofObligationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getName(), that.getName());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("name", this.getName())
			       .add("description", this.getDescription())
			       .toString();
	}
}
