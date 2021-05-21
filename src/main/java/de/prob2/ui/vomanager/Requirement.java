package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.prob2.ui.verifications.Checked;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
        "type",
        "text"
})
public class Requirement {

    public enum RequirementType {
        INVARIANT("Invariant Requirement"),
        SAFETY("Safety Requirement"),
        LIVENESS("Liveness Requirement"),
        USE_CASE("Use Case Requirement");

        private String name;

        RequirementType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }



    private RequirementType type;

    private String text;

    @JsonIgnore
    private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    @JsonCreator
    public Requirement(@JsonProperty("type") RequirementType type,
                       @JsonProperty("text") String text) {
        this.type = type;
        this.text = text;
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

    public void setType(RequirementType type) {
        this.type = type;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void reset() {
        // TODO: Implement
    }
}
