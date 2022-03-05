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

import java.util.List;
import java.util.Objects;


@JsonPropertyOrder({
		"id",
		"context",
		"taskType",
		"parameters",
		"item"
})
public class ValidationTask {

	private final String id;

	private final String context;

	private final ValidationTechnique validationTechnique;

	private final List<String> parameters;

	private final Object item;

	@JsonIgnore
	private IExecutableItem executable;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public ValidationTask(@JsonProperty("id") String id, @JsonProperty("context") String context,
						  @JsonProperty("validationTechnique") ValidationTechnique validationTechnique, @JsonProperty("parameters") List<String> parameters,
						  @JsonProperty("item") Object item) {
		this.id = id;
		this.context = context;
		this.validationTechnique = validationTechnique;
		this.parameters = parameters;
		this.item = item;
	}

	public String getId() {
		return id;
	}

	public String getContext() {
		return context;
	}

	public ValidationTechnique getValidationTechnique() {
		return validationTechnique;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public String getRepresentation() {
		if(parameters.isEmpty()) {
			return String.format("%s/%s/%s", id, context, validationTechnique);
		}
		return String.format("%s/%s/%s: %s", id, context, validationTechnique, String.join(", ", parameters));
	}

	public void setExecutable(IExecutableItem executable) {
		this.executable = executable;
		checked.unbind();
		if(item != null) {
			checked.bind(executable.checkedProperty());
		}
	}

	public IExecutableItem getExecutable() {
		return executable;
	}

	public Object getItem() {
		return item;
	}

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public void reset() {
		// TODO: Implement
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = ModelCheckingItem.class, name = "ModelCheckingItem"),
			@JsonSubTypes.Type(value = LTLFormulaItem.class, name = "LTLFormulaItem"),
			@JsonSubTypes.Type(value = String.class, name = "Path"),
	})
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValidationTask that = (ValidationTask) o;
		return Objects.equals(id, that.id) && Objects.equals(context, that.context) && validationTechnique == that.validationTechnique && Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, context, validationTechnique, parameters);
	}

	@Override
	public String toString() {
		return getRepresentation();
	}
}
