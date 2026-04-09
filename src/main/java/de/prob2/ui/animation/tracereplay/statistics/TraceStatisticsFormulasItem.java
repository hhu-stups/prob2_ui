package de.prob2.ui.animation.tracereplay.statistics;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IValidationTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"operation",
	"computations",
	"desiredEffects",
	"evaluateChanged",
	"selected",
})
public class TraceStatisticsFormulasItem implements IValidationTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceStatisticsFormulasItem.class);

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String id;
	private final String operation;
	private final String computations;
	private final String desiredEffects;
	private boolean evaluateChanged;
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status;

	@JsonCreator
	public TraceStatisticsFormulasItem(
			@JsonProperty("id") final String id,
			@JsonProperty("operation") final String operation,
			@JsonProperty("computations") final String computations,
			@JsonProperty("desiredEffects") final String desiredEffects,
			@JsonProperty("evaluateChanged") final Boolean evaluateChanged
	) {
		this.id = id;
		this.operation = Objects.requireNonNull(operation, "operation");
		this.computations = Objects.requireNonNull(computations, "computations");
		this.desiredEffects = Objects.requireNonNull(desiredEffects, "desiredEffects");
		this.evaluateChanged = evaluateChanged == null ? false : evaluateChanged;
		this.status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@JsonIgnore
	public String getOperationWithId() {
		if(id == null) {
			return operation;
		}
		return String.format("[%s] %s", id, operation);
	}

	public String getOperation() {
		return operation;
	}

	public String getComputations() {
		return computations;
	}

	public String getDesiredEffects() {
		return desiredEffects;
	}

	public boolean evaluateChanged() {
		return evaluateChanged;
	}

	@Override
	public ValidationTaskType<TraceStatisticsFormulasItem> getTaskType() {
		return BuiltinValidationTaskTypes.TRACE_STATISTICS_FORMULAS;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("animation.tracereplay.statistics.task.name");
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		return "Operation: " + this.getOperation() + ", Computations: " + this.getComputations() + ", Desired Effects: " + this.getDesiredEffects() + ", Evaluate Changed: " + this.evaluateChanged();
	}

	@Override
	public ObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	public void setStatus(final CheckingStatus status) {
		this.statusProperty().set(status);
	}

	@Override
	public CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		// TODO: Check formula manually similar to VisualizationFormulaTask
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void resetAnimatorDependentState() {}

	@Override
	public void reset() {
		this.setStatus(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public TraceStatisticsFormulasItem copy() {
		return new TraceStatisticsFormulasItem(this.id, this.operation, this.computations, this.desiredEffects, this.evaluateChanged);
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TraceStatisticsFormulasItem that
				&& Objects.equals(this.getId(), that.getId())
				&& Objects.equals(this.getOperation(), that.getOperation())
				&& Objects.equals(this.getComputations(), that.getComputations())
				&& Objects.equals(this.getDesiredEffects(), that.getDesiredEffects())
				&& Objects.equals(this.evaluateChanged(), that.evaluateChanged());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", this.getId())
				.add("operation", this.getOperation())
				.add("computations", this.getComputations())
				.add("desiredEffects", this.getDesiredEffects())
				.add("evaluateChanged", this.evaluateChanged())
				.toString();
	}

}
