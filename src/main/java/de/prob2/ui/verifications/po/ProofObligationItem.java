package de.prob2.ui.verifications.po;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.model.eventb.ProofObligation;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ProofObligationItem implements IValidationTask {

	private String id;
	private final String name;
	@JsonIgnore
	private final String description;
	@JsonIgnore
	private final ObjectProperty<Checked> checked;

	@JsonCreator
	public ProofObligationItem(
			@JsonProperty("id") final String id,
			@JsonProperty("name") final String name
	) {
		super();
		this.id = id;
		this.name = name;
		this.description = "";
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.PARSE_ERROR);
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		super();
		this.id = null;
		this.name = proofObligation.getName();
		this.description = proofObligation.getDescription();
		this.checked = new SimpleObjectProperty<>(this, "checked", proofObligation.isDischarged() ? Checked.SUCCESS : Checked.NOT_CHECKED);
	}

	@Override
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

	@JsonIgnore
	@Override
	public String toString() {
		return new StringJoiner(", ", ProofObligationItem.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("name='" + name + "'")
				.add("description='" + description + "'")
				.add("checked=" + getChecked())
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProofObligationItem that = (ProofObligationItem) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description)
			&& Objects.equals(this.getChecked(), that.getChecked());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, getChecked());
	}
}
