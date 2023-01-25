package de.prob2.ui.dynamic;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class DynamicCommandFormulaItem implements IValidationTask {

	private String id;
	private final String commandType;
	private String formula;
	@JsonIgnore
	private final ObjectProperty<Checked> checked;

	@JsonCreator
	public DynamicCommandFormulaItem(
			@JsonProperty("id") final String id,
			@JsonProperty("commandType") final String commandType,
			@JsonProperty("formula") final String formula
	) {
		super();
		this.id = id;
		this.commandType = commandType;
		this.formula = formula;
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		if (this.getCommandType().isEmpty()) {
			return this.getCommandType();
		} else {
			return this.getCommandType() + " // " + getFormula();
		}
	}

	public String getCommandType() {
		return commandType;
	}

	public void setFormula(String formula) {
		this.formula = formula;
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
	public String toString() {
		return new StringJoiner(", ", DynamicCommandFormulaItem.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("commandType='" + commandType + "'")
				.add("formula='" + formula + "'")
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DynamicCommandFormulaItem that = (DynamicCommandFormulaItem) o;
		return Objects.equals(id, that.id) && Objects.equals(commandType, that.commandType) && Objects.equals(formula, that.formula);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, commandType, formula);
	}
}
