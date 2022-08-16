package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

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
	private IValidationExpression parsedExpression;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonIgnore
	private final ObservableList<IValidationTask> tasks = FXCollections.observableArrayList();

	@JsonCreator
	public ValidationObligation(@JsonProperty("id") String id,
								@JsonProperty("expression") String expression,
								@JsonProperty("requirement") String requirement) {
		this(id, expression, requirement, Collections.emptyList());
	}


	public ValidationObligation(String id, String expression, String requirement, List<ValidationObligation> previousVersions) {
		this.id = id;
		this.expression = expression;
		this.requirement = requirement;
		this.previousVersions = previousVersions;

		final InvalidationListener checkedListener = o -> {
			if (this.parsedExpression == null) {
				this.checked.set(Checked.PARSE_ERROR);
			} else {
				this.checked.set(this.parsedExpression.getChecked());
			}
		};
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

	public void setParsedExpression(final IValidationExpression expression) {
		this.parsedExpression = expression;
		if (expression == null) {
			this.checked.set(Checked.PARSE_ERROR);
			this.getTasks().clear();
		} else {
			this.getTasks().setAll(expression.getAllTasks()
				.map(ValidationTaskExpression::getTask)
				.collect(Collectors.toList()));
		}
	}

	public ValidationObligation changeRequirement(String requirement) {
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

	public IValidationExpression getParsedExpression() {
		return parsedExpression;
	}

	public String getRequirement() {
		return requirement;
	}

	@JsonIgnore
	public List<ValidationObligation> getPreviousVersions() {
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
		return String.format(Locale.ROOT, "ValidationObligation{checked = %s, id = %s, expression = %s, requirement = %s}", checked.get(), id, expression, requirement);
	}

}
