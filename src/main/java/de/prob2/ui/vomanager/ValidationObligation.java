package de.prob2.ui.vomanager;


import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.voparser.node.PVo;
import de.prob2.ui.verifications.Checked;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class ValidationObligation implements IAbstractRequirement, INameable {

	private final String id;

	private final String expression;

	private final String requirement;

	@JsonIgnore
	private final List<ValidationObligation> previousVersions;

	@JsonIgnore
	private PVo expressionAst;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonIgnore
	private final ObservableList<IValidationTask> tasks = FXCollections.observableArrayList();

	@JsonCreator
	public ValidationObligation(@JsonProperty("id") String id,
								@JsonProperty("expression") String expression,
								@JsonProperty("requirement") String requirement) {
		this.id = id;
		this.expression = expression;
		this.requirement = requirement;
		this.previousVersions = Collections.emptyList();
	}


	public ValidationObligation(String id, String expression, String requirement, List<ValidationObligation> previousVersions) {
		this.id = id;
		this.expression = expression;
		this.requirement = requirement;
		this.previousVersions = previousVersions;
	}

	public void setExpressionAst(PVo expressionAst, VOChecker voChecker) {
		this.expressionAst = expressionAst;
		addCheckedListener(voChecker);
	}

	private void addCheckedListener(VOChecker voChecker) {
		final InvalidationListener checkedListener = o -> this.checked.set(voChecker.updateVOExpression(expressionAst, this));
		this.getTasks().addListener((ListChangeListener<IValidationTask>)o -> {
			while (o.next()) {
				if (o.wasRemoved()) {
					for (final IValidationTask task : o.getRemoved()) {
						task.checkedProperty().removeListener(checkedListener);
					}
				}
				if (o.wasAdded()) {
					for (final IValidationTask task : o.getAddedSubList()) {
						task.checkedProperty().addListener(checkedListener);
					}
				}
				checkedListener.invalidated(null);
			}
		});
	}


	public ValidationObligation changeRequirement(String requirement){
		return new ValidationObligation(this.id, this.expression, requirement, this.previousVersions);
	}


	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	public ObservableList<IValidationTask> getTasks() {
		return this.tasks;
	}

	public String getId() {
		return id;
	}

	public String getExpression() {
		return expression;
	}

	public PVo getExpressionAst() {
		return expressionAst;
	}

	public String getRequirement() {
		return requirement;
	}

	@JsonIgnore
	public List<ValidationObligation> getPreviousVersions(){
		return previousVersions;
	}


	@Override
	@JsonIgnore
	public String getName() {
		if (this.getId() == null) {
			return this.getExpression();
		} else {
			return "[" + this.getId() + "] " + this.getExpression();
		}
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
		return String.format("ValidationObligation{checked = %s, id = %s, expression = %s, requirement = %s}", checked.get(), id, expression, requirement);
	}

}
