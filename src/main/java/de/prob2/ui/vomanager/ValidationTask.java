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
		"validationTechnique",
		"parameters",
		"item"
})
public class ValidationTask {

	private String id;

	private String context;

	private ValidationTechnique validationTechnique;

	private String parameters;

	private Object item;

	@JsonIgnore
	private IExecutableItem executable;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public ValidationTask(@JsonProperty("id") String id, @JsonProperty("context") String context,
						  @JsonProperty("validationTechnique") ValidationTechnique validationTechnique, @JsonProperty("parameters") String parameters,
						  @JsonProperty("item") Object item) {
		this(context, validationTechnique, parameters, item);
		this.id = id;
	}

	public ValidationTask(String context, ValidationTechnique validationTechnique, String parameters,
						  Object item) {
		this.context = context;
		this.validationTechnique = validationTechnique;
		this.parameters = parameters;
		this.item = item;
		setExecutable((IExecutableItem) item);
	}

	public void setId(String id) {
		this.id = id;
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

	public void setValidationTechnique(ValidationTechnique validationTechnique) {
		this.validationTechnique = validationTechnique;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	@JsonIgnore
	public String getPrefix() {
		return String.format("%s/%s/%s", id, context, validationTechnique.getId());
	}

	@JsonIgnore
	public String getRepresentation() {
		if(parameters.isEmpty()) {
			return String.format("%s/%s/%s", id, context, validationTechnique.getId());
		}
		return String.format("%s/%s/%s: %s", id, context, validationTechnique.getId(), String.join(", ", parameters));
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

	@JsonIgnore
	public Object getItem() {
		return item;
	}

	public void setItem(Object item) {
		this.item = item;
		setExecutable((IExecutableItem) item);
	}

	public void setContext(String context) {
		this.context = context;
	}

	public Checked getChecked() {
		return checked.get();
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
