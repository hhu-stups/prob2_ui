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
	"id",
	"expression",
	"requirement"
})
public class ValidationObligation implements IAbstractRequirement {

	private String id;

	private String expression;

	private String requirement;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonCreator
	public ValidationObligation(@JsonProperty("id") String id,
								@JsonProperty("expression") String expression,
								@JsonProperty("requirement") String requirement) {
		this.id = id;
		this.expression = expression;
		this.requirement = requirement;
	}

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public void setRequirement(String requirement) {
		this.requirement = requirement;
	}

	public String getRequirement() {
		return requirement;
	}

	@JsonIgnore
	public String getName() {
		return getId();
	}

	@JsonIgnore
	public String getConfiguration() {
		return getExpression();
	}

	public void reset() {
		// TODO: Implement
	}

	/*
	* This function is used for the requirements table view
	*/
	@JsonIgnore
	public String getShortTypeName() {
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValidationObligation that = (ValidationObligation) o;
		return Objects.equals(id, that.id) && Objects.equals(expression, that.expression) && Objects.equals(requirement, that.requirement);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, expression, requirement);
	}

	@Override
	public String toString() {
		return String.format("ValidationObligation{checked = %s, id = %s, expression = %s, requirement = %s}", checked, id, expression, requirement);
	}

}
