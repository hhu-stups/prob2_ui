package de.prob2.ui.vomanager;

import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public final class ValidationObligation implements INameable {
	private final String machine;

	private final String expression;

	@JsonIgnore
	private final ValidationObligation parent ;

	@JsonIgnore
	private IValidationExpression parsedExpression;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonIgnore
	private final ObservableList<IValidationTask> tasks = FXCollections.observableArrayList();

	@JsonCreator
	public ValidationObligation(
		@JsonProperty("machine") String machine,
		@JsonProperty("expression") String expression
	) {
		this(machine, expression, null);
	}





	public ValidationObligation(String machine, String expression, ValidationObligation parent) {
		this.machine = machine;
		this.expression = expression;
		this.parent = parent;

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

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	public ObservableList<IValidationTask> getTasks() {
		return this.tasks;
	}

	public String getMachine() {
		return this.machine;
	}

	public String getExpression() {
		return expression;
	}

	public IValidationExpression getParsedExpression() {
		return parsedExpression;
	}

	@Override
	@JsonIgnore
	public String getName() {
		return this.getExpression();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final ValidationObligation other = (ValidationObligation)obj;
		return this.getMachine().equals(other.getMachine())
			&& this.getExpression().equals(other.getExpression());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getMachine(), this.getExpression());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("machine", machine)
			.add("expression", expression)
			.toString();
	}

	@JsonIgnore //TODO Fix this when making history and refinement saving persistent
	public ValidationObligation getParent() {
		return parent;
	}
}
