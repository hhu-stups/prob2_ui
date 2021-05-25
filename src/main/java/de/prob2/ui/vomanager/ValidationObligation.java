package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

@JsonPropertyOrder({
        "task",
        "configuration",
        "item"
})
public class ValidationObligation {

    private final ValidationTask task;

    private final String configuration;

    private IExecutableItem item;

    @JsonIgnore
    private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    @JsonCreator
    public ValidationObligation(@JsonProperty("task") ValidationTask task,
                                @JsonProperty("text") String configuration,
                                @JsonProperty("item") IExecutableItem item) {
        this.task = task;
        this.configuration = configuration;
        this.item = item;
        checked.bind(item.checkedProperty());
    }

    public ObjectProperty<Checked> checkedProperty() {
        return checked;
    }

    public Checked getChecked() {
        return checked.get();
    }

    public ValidationTask getTask() {
        return task;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setItem(IExecutableItem item) {
        this.item = item;
        checked.unbind();
        if(item != null) {
            checked.bind(item.checkedProperty());
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ModelCheckingItem.class, name = "ModelCheckingItem"),
            @JsonSubTypes.Type(value = LTLFormulaItem.class, name = "LTLFormulaItem"),
    })
    public IExecutableItem getItem() {
        return item;
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
