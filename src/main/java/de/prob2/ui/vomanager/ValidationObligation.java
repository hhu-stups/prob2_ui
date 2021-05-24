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
        "task",
        "configuration"
})
public class ValidationObligation {

    public enum ValidationTask {
        MODEL_CHECKING("Model Checking"),
        LTL_MODEL_CHECKING("LTL Model Checking"),
        SYMBOLIC_MODEL_CHECKING("Symbolic Model Checking"),
        TRACE_REPLAY("Trace Replay");

        private final String name;

        ValidationTask(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final ValidationTask task;

    private final String configuration;

    @JsonIgnore
    private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    @JsonCreator
    public ValidationObligation(@JsonProperty("task") ValidationTask task,
                                @JsonProperty("text") String configuration) {
        this.task = task;
        this.configuration = configuration;
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

    public ValidationTask getTask() {
        return task;
    }

    public String getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationObligation that = (ValidationObligation) o;
        return task == that.task && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, configuration);
    }

    @Override
    public String toString() {
        return String.format("ValidationObligation{checked = %s, task = %s, configuration = %s}", checked, task, configuration);
    }

}
