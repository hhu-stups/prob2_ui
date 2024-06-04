package de.prob2.ui.dynamic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"commandType",
	"formula",
})
public final class VisualizationFormulaTask implements IValidationTask {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String commandType;
	private final String formula;
	@JsonIgnore
	private final ObjectProperty<Checked> checked;

	@JsonCreator
	public VisualizationFormulaTask(
			@JsonProperty("id") final String id,
			@JsonProperty("commandType") final String commandType,
			@JsonProperty("formula") final String formula
	) {
		this.id = id;
		this.commandType = Objects.requireNonNull(commandType, "commandType");
		this.formula = Objects.requireNonNull(formula, "formula");
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ValidationTaskType<VisualizationFormulaTask> getTaskType() {
		return BuiltinValidationTaskTypes.VISUALIZATION_FORMULA;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return this.getCommandType();
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getFormula();
	}

	public String getCommandType() {
		return commandType;
	}

	public String getFormula() {
		return formula;
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}

	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	public void setChecked(final Checked checked) {
		this.checkedProperty().set(checked);
	}

	@Override
	public void resetAnimatorDependentState() {}

	@Override
	public void reset() {
		this.setChecked(Checked.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof VisualizationFormulaTask that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getCommandType(), that.getCommandType())
			       && Objects.equals(this.getFormula(), that.getFormula());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("commandType", this.getCommandType())
			       .add("formula", this.getFormula())
			       .toString();
	}
}
