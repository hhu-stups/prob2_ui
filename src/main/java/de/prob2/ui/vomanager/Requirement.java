package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
	"name",
	"type",
	"text",
	"validationObligations"
})
public class Requirement {

	private String name;

	private RequirementType type;

	private String text;

	private final ListProperty<ValidationObligation> validationObligations = new SimpleListProperty<>(FXCollections.observableArrayList());

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonIgnore
	private final ChangeListener<Checked> listener = (observable, from, to) -> updateChecked();

	@JsonCreator
	public Requirement(@JsonProperty("name") String name,
			@JsonProperty("type") RequirementType type,
			@JsonProperty("text") String text,
			@JsonProperty("validationObligations") List<ValidationObligation> validationObligations) {
		this.name = name;
		this.type = type;
		this.text = text;
		this.validationObligations.get().addAll(validationObligations);
		initListeners();;
	}

	public void updateChecked() {
		List<ValidationObligation> validationObligations = this.validationObligationsProperty();
		if (validationObligations.isEmpty()) {
			this.checked.set(Checked.NOT_CHECKED);
		} else {
			final boolean failed = validationObligations.stream()
					.map(ValidationObligation::getChecked)
					.anyMatch(Checked.FAIL::equals);
			final boolean success = !failed && validationObligations.stream()
					.map(ValidationObligation::getChecked)
					.allMatch(Checked.SUCCESS::equals);
			final boolean timeout = !failed && validationObligations.stream()
					.map(ValidationObligation::getChecked)
					.anyMatch(Checked.TIMEOUT::equals);

			if (success) {
				this.checked.set(Checked.SUCCESS);
			} else if (failed) {
				this.checked.set(Checked.FAIL);
			} else if (timeout) {
				this.checked.set(Checked.TIMEOUT);
			} else {
				this.checked.set(Checked.NOT_CHECKED);
			}
		}
	}

	private void initListeners() {
		this.validationObligationsProperty().addListener((o, from, to) -> updateChecked());
	}

	public String getName() {
		return name;
	}

	public RequirementType getType() {
		return type;
	}

	@JsonIgnore
	public String getShortTypeName() {
		return type.name();
	}

	public String getText() {
		return text;
	}

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	public void addValidationObligation(ValidationObligation validationObligation) {
		validationObligationsProperty().add(validationObligation);
		validationObligation.checkedProperty().addListener(listener);
	}

	public void removeValidationObligation(ValidationObligation validationObligation) {
		validationObligation.checkedProperty().removeListener(listener);
		validationObligationsProperty().remove(validationObligation);
	}

	public ListProperty<ValidationObligation> validationObligationsProperty() {
		return validationObligations;
	}

	public List<ValidationObligation> getValidationObligations() {
		return validationObligations.get();
	}

	public void setChecked(Checked checked) {
		this.checked.set(checked);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(RequirementType type) {
		this.type = type;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void reset() {
		// TODO: Implement
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Requirement that = (Requirement) o;
		return Objects.equals(name, that.name) && type == that.type && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, text);
	}

	@Override
	public String toString() {
		return String.format("Requirement{checked = %s, name = %s, type = %s, text = %s, validationObligations = %s}", checked, name, type, text, validationObligations.get());
	}
}
