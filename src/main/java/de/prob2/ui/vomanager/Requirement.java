package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

@JsonPropertyOrder({
	"name",
	"introducedAt",
	"type",
	"text"
})
public class Requirement implements IAbstractRequirement, INameable {

	private String name;

	private String introducedAt; // machine where the requirement is first introduced

	private RequirementType type;

	private String text;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public Requirement(@JsonProperty("name") String name,
			@JsonProperty("introducedAt") String introducedAt,
			@JsonProperty("type") RequirementType type,
			@JsonProperty("text") String text) {
		this.name = name;
		this.introducedAt = introducedAt;
		this.type = type;
		this.text = text;
	}


	@Override
	public String getName() {
		return name;
	}

	public String getIntroducedAt() {
		return introducedAt;
	}

	public RequirementType getType() {
		return type;
	}

	@JsonIgnore
	public String getConfiguration() {
		return getText();
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

	public void setChecked(Checked checked) {
		this.checked.set(checked);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setIntroducedAt(String introducedAt) {
		this.introducedAt = introducedAt;
	}

	public void setType(RequirementType type) {
		this.type = type;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void reset() {

	}

	public void setData(String name, String introducedAt, RequirementType type, String text) {
		this.name = name;
		this.introducedAt = introducedAt;
		this.type = type;
		this.text = text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Requirement that = (Requirement) o;
		return Objects.equals(name, that.name) && Objects.equals(introducedAt, that.introducedAt) && type == that.type && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, introducedAt, type, text);
	}

	@Override
	public String toString() {
		return String.format("Requirement{checked = %s, name = %s, introducedAt = %s, type = %s, text = %s}", checked, name, introducedAt, type, text);
	}
}
