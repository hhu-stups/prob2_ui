package de.prob2.ui.vomanager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import de.prob.check.ModelCheckingOptions;
import de.prob.voparser.VOException;
import de.prob.voparser.VOParser;
import de.prob.voparser.VTType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.vomanager.ast.IValidationExpression;
import de.prob2.ui.vomanager.ast.ValidationTaskExpression;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public final class ValidationObligation {
	private final String machine;

	private final String expression;

	@JsonIgnore
	private final ValidationObligation parent;

	@JsonIgnore
	private IValidationExpression parsedExpression;

	@JsonIgnore
	private final ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	@JsonIgnore
	private final ObservableList<IValidationTask<?>> tasks = FXCollections.observableArrayList();

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
				this.checked.set(Checked.INVALID_TASK);
			} else {
				this.checked.set(this.parsedExpression.getChecked());
			}
		};
		this.getTasks().addListener((ListChangeListener<IValidationTask<?>>) o -> {
			while (o.next()) {
				if (o.wasRemoved()) {
					for (final IValidationTask<?> task : o.getRemoved()) {
						task.checkedProperty().removeListener(checkedListener);
					}
				}
				if (o.wasAdded()) {
					for (final IValidationTask<?> task : o.getAddedSubList()) {
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
			this.checked.set(Checked.INVALID_TASK);
			this.getTasks().clear();
		} else {
			this.getTasks().setAll(expression.getAllTasks()
				                       .map(ValidationTaskExpression::getTask)
				                       .collect(Collectors.toList()));
		}
	}

	private static VTType extractType(IValidationTask<?> validationTask) {
		if (validationTask instanceof ReplayTrace) {
			return VTType.TRACE;
		} else if (validationTask instanceof SimulationItem) {
			SimulationType simulationType = ((SimulationItem) validationTask).getType();
			if (simulationType == SimulationType.MONTE_CARLO_SIMULATION || simulationType == SimulationType.HYPOTHESIS_TEST || simulationType == SimulationType.ESTIMATION) {
				return VTType.EXPLORE;
			}
			// TODO: Implement a single simulation
			return VTType.TRACE;
		} else if (validationTask instanceof TemporalFormulaItem) {
			return VTType.EXPLORE;
		} else if (validationTask instanceof ModelCheckingItem) {
			Set<ModelCheckingOptions.Options> options = ((ModelCheckingItem) validationTask).getOptions();
			if (options.contains(ModelCheckingOptions.Options.FIND_GOAL) ||
				    ((ModelCheckingItem) validationTask).getGoal() == null ||
				    !((ModelCheckingItem) validationTask).getGoal().isEmpty()) {
				return VTType.TRACE;
			}
			// Otherwise invariant/deadlock checking, or just covering state space
			return VTType.EXPLORE;
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			return VTType.STATIC;
		} else if (validationTask instanceof ProofObligationItem) {
			return VTType.STATIC;
		} else if (validationTask instanceof DynamicFormulaTask) {
			return VTType.STATE_SPACE;
		}
		return null;
	}

	public void parse(final List<IValidationTask<?>> validationTasksList) {
		Map<String, IValidationTask<?>> validationTasks = validationTasksList.stream()
			                                                  .filter(vt -> vt.getId() != null)
			                                                  .collect(Collectors.toMap(IValidationTask::getId, Function.identity()));

		final VOParser voParser = new VOParser();
		validationTasks.forEach((id, vt) -> voParser.registerTask(id, extractType(vt)));
		try {
			final IValidationExpression parsed = IValidationExpression.parse(voParser, this.getExpression());
			parsed.getAllTasks().forEach(taskExpr -> {
				IValidationTask<?> validationTask;
				if (validationTasks.containsKey(taskExpr.getIdentifier())) {
					validationTask = validationTasks.get(taskExpr.getIdentifier());
				} else {
					validationTask = new ValidationTaskNotFound(taskExpr.getIdentifier());
				}
				taskExpr.setTask(validationTask);
			});
			this.setParsedExpression(parsed);
		} catch (VOException e) {
			this.setParsedExpression(null);
			throw e;
		}
	}

	public void parse(final Machine machine) {
		this.parse(machine.getMachineProperties().getValidationTasksWithId());
	}

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	public ObservableList<IValidationTask<?>> getTasks() {
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
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final ValidationObligation other = (ValidationObligation) obj;
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
