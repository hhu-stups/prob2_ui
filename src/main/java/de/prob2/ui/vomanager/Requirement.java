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
        "type",
        "text"
})
public class Requirement {

    public enum RequirementType {
        INVARIANT("Invariant Requirement"),
        SAFETY("Safety Requirement"),
        LIVENESS("Liveness Requirement"),
        USE_CASE("Use Case Requirement");

        private final String name;

        RequirementType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private String name;

    private RequirementType type;

    private String text;

    @JsonIgnore
    private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    @JsonCreator
    public Requirement(@JsonProperty("name") String name,
                       @JsonProperty("type") RequirementType type,
                       @JsonProperty("text") String text) {
        this.name = name;
        this.type = type;
        this.text = text;
    }

    public String getName() {
        return name;
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
        return String.format("Requirement{checked = %s, name = %s, type = %s, text = %s}", checked, name, type, text);
    }
}
