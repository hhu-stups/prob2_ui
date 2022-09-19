package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.Checked;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Requirement implements INameable {

	private final String name;

	private final String introducedAt; // machine where the requirement is first introduced

	private final RequirementType type;

	private final String text;

	private final List<Requirement> previousVersions;

	private final Requirement parent;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public Requirement(@JsonProperty("name") String name,
			@JsonProperty("introducedAt") String introducedAt,
			@JsonProperty("type") RequirementType type,
			@JsonProperty("text") String text) {
		this(name, introducedAt, type, text, Collections.emptyList(), null);
	}

	public Requirement(String name, String introducedAt, RequirementType type, String text, List<Requirement> previousVersions, Requirement parent) {
		this.name = name;
		this.introducedAt = introducedAt;
		this.type = type;
		this.text = text;
		this.previousVersions = previousVersions;
		this.parent = parent;
	}


	@JsonIgnore
	public List<Requirement> getPreviousVersions(){
		return previousVersions;
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
		return String.format(Locale.ROOT, "Requirement{checked = %s, name = %s, introducedAt = %s, type = %s, text = %s}", checked, name, introducedAt, type, text);
	}

	@JsonIgnore //TODO Fix this when making history and refinement saving persistent
	public Requirement getParent(){
		return parent;
	}
}
