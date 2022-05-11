package de.prob2.ui.vomanager;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
		"id",
		"context",
		"validationTechnique",
		"parameters"
})
public class ValidationTask implements INameable {

	private String id;

	private String context;

	private ValidationTechnique validationTechnique;

	private String parameters;

	@JsonIgnore
	private IExecutableItem executable;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public ValidationTask(@JsonProperty("id") String id, @JsonProperty("context") String context,
						  @JsonProperty("validationTechnique") ValidationTechnique validationTechnique, @JsonProperty("parameters") String parameters) {
		this(id, context, validationTechnique, parameters, null);
	}

	public ValidationTask(ValidationTechnique validationTechnique, String parameters, IExecutableItem executable) {
		this(null, null, validationTechnique, parameters, executable);
	}

	public ValidationTask(String id, String context, ValidationTechnique validationTechnique, String parameters, IExecutableItem executable) {
		this.id = id;
		this.context = context;
		this.validationTechnique = validationTechnique;
		this.parameters = parameters;
		setExecutable(executable);
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

	@Override
	@JsonIgnore
	public String getName() {
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
		if(executable != null) {
			checked.bind(executable.checkedProperty());
		}
	}

	public IExecutableItem getExecutable() {
		return executable;
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

	public void setData(String id, IExecutableItem executable, String context, String parameters) {
		this.id = id;
		this.executable = executable;
		this.context = context;
		this.parameters = parameters;
	}

	public void reset() {

	}

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
