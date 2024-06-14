package de.prob2.ui.verifications.modelchecking;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

@JsonPropertyOrder({
	"id",
	"searchStrategy",
	"nodesLimit",
	"timeLimit",
	"options",
	"goal",
	"selected",
})
public final class ModelCheckingItem implements IExecutableItem {
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	private final ModelCheckingSearchStrategy searchStrategy;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer nodesLimit;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer timeLimit;

	private final Set<ModelCheckingOptions.Options> options;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String goal;

	private final BooleanProperty selected;

	@JsonIgnore
	private final ListProperty<ModelCheckingStep> steps = new SimpleListProperty<>(this, "steps", FXCollections.observableArrayList());

	@JsonIgnore
	private final ObjectProperty<ModelCheckingStep> currentStep = new SimpleObjectProperty<>(this, "currentStep", null);

	@JsonCreator
	public ModelCheckingItem(
		@JsonProperty("id") final String id,
		@JsonProperty("searchStrategy") final ModelCheckingSearchStrategy searchStrategy,
		@JsonProperty("nodesLimit") final Integer nodesLimit,
		@JsonProperty("timeLimit") final Integer timeLimit,
		@JsonProperty("goal") final String goal,
		@JsonProperty("options") final Set<ModelCheckingOptions.Options> options
	) {
		this.id = id;
		this.searchStrategy = Objects.requireNonNull(searchStrategy, "searchStrategy");
		this.nodesLimit = nodesLimit;
		this.timeLimit = timeLimit;
		this.goal = goal;
		this.options = Objects.requireNonNull(options, "options");
		this.selected = new SimpleBooleanProperty(true);
		this.initListeners();
	}

	private void initListeners() {
		this.stepsProperty().addListener((o, from, to) -> {
			if (to.isEmpty()) {
				this.status.set(CheckingStatus.NOT_CHECKED);
			} else {
				final boolean failed = to.stream()
					                       .map(ModelCheckingStep::getStatus)
					                       .anyMatch(CheckingStatus.FAIL::equals);
				final boolean success = !failed && to.stream()
					                                   .map(ModelCheckingStep::getStatus)
					                                   .anyMatch(CheckingStatus.SUCCESS::equals);

				if (success) {
					this.status.set(CheckingStatus.SUCCESS);
				} else if (failed) {
					this.status.set(CheckingStatus.FAIL);
				} else {
					this.status.set(CheckingStatus.TIMEOUT);
				}
			}
		});
	}

	@Override
	public ReadOnlyObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ValidationTaskType<ModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.MODEL_CHECKING;
	}

	public ModelCheckingSearchStrategy getSearchStrategy() {
		return this.searchStrategy;
	}

	public Integer getNodesLimit() {
		return nodesLimit;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}

	public String getGoal() {
		return goal;
	}

	public Set<ModelCheckingOptions.Options> getOptions() {
		return this.options;
	}

	public ModelCheckingOptions getFullOptions(final AbstractModel model) {
		ModelCheckingOptions fullOptions = new ModelCheckingOptions(this.getOptions())
			                                   .searchStrategy(this.getSearchStrategy())
			                                   // Start checking from the beginning if this item hasn't been checked yet,
			                                   // otherwise continue checking from the last error.
			                                   .recheckExisting(this.getSteps().isEmpty());
		if (this.getGoal() != null) {
			fullOptions = fullOptions.customGoal(model.parseFormula(this.getGoal()));
		}
		if (this.getNodesLimit() != null) {
			fullOptions = fullOptions.stateLimit(this.getNodesLimit());
		}
		if (this.getTimeLimit() != null) {
			fullOptions = fullOptions.timeLimit(Duration.ofSeconds(this.getTimeLimit()));
		}
		return fullOptions;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("verifications.modelchecking.type");
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		final StringJoiner s = new StringJoiner(", ");
		final String strategyKey = ModelcheckingStage.getSearchStrategyNameKey(this.getSearchStrategy());
		if (strategyKey != null) {
			s.add(i18n.translate(strategyKey));
		} else {
			s.add(this.getSearchStrategy().toString());
		}
		if (this.getNodesLimit() != null) {
			s.add(i18n.translate("verifications.modelchecking.description.nodeLimit", this.getNodesLimit()));
		}
		if (this.getTimeLimit() != null) {
			s.add(i18n.translate("verifications.modelchecking.description.timeLimit", this.getTimeLimit()));
		}
		Set<ModelCheckingOptions.Options> opts = this.getOptions();
		for (ModelCheckingOptions.Options opt : ModelCheckingOptions.Options.values()) {
			boolean expectedContains = opt != ModelCheckingOptions.Options.IGNORE_OTHER_ERRORS;
			if (opts.contains(opt) == expectedContains) {
				s.add(i18n.translate("verifications.modelchecking.description.option." + opt.getPrologName()));
			}
		}
		if (this.getGoal() != null) {
			s.add(i18n.translate("verifications.modelchecking.description.additionalGoal", this.getGoal()));
		}
		return s.toString();
	}

	@Override
	@JsonProperty("selected")
	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}

	@JsonProperty("selected")
	@Override
	public boolean selected() {
		return selected.get();
	}

	@Override
	public BooleanProperty selectedProperty() {
		return selected;
	}

	public ListProperty<ModelCheckingStep> stepsProperty() {
		return steps;
	}

	public List<ModelCheckingStep> getSteps() {
		return steps.get();
	}

	public ObjectProperty<ModelCheckingStep> currentStepProperty() {
		return this.currentStep;
	}

	public ModelCheckingStep getCurrentStep() {
		return this.currentStepProperty().get();
	}

	public void setCurrentStep(final ModelCheckingStep currentStep) {
		this.currentStepProperty().set(currentStep);
	}

	@Override
	public void execute(final ExecutionContext context) {
		Modelchecker.executeIfNeeded(this, context.stateSpace());
	}

	@Override
	public void resetAnimatorDependentState() {
		// Clearing the steps list causes the listener to reset the status,
		// but in this case we want to preserve the status.
		CheckingStatus savedChecked = this.getStatus();
		this.stepsProperty().clear();
		this.status.set(savedChecked);
		this.currentStep.set(null);
	}

	@Override
	public void reset() {
		this.status.set(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ModelCheckingItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getSearchStrategy(), that.getSearchStrategy())
			       && Objects.equals(this.getNodesLimit(), that.getNodesLimit())
			       && Objects.equals(this.getTimeLimit(), that.getTimeLimit())
			       && Objects.equals(this.getOptions(), that.getOptions())
			       && Objects.equals(this.getGoal(), that.getGoal());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s,%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getSearchStrategy(), this.getNodesLimit(), this.getTimeLimit(), this.getGoal(), this.getOptions());
	}
}
