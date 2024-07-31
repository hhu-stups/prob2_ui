package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.animator.command.GetStatisticsCommand;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.application.Platform;

@JsonPropertyOrder({
	"id",
	"searchStrategy",
	"nodesLimit",
	"timeLimit",
	"options",
	"goal",
	"selected",
})
public final class ProBModelCheckingItem extends ModelCheckingItem {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final ModelCheckingSearchStrategy searchStrategy;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer nodesLimit;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer timeLimit;

	private final Set<ModelCheckingOptions.Options> options;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String goal;

	@JsonCreator
	public ProBModelCheckingItem(
		@JsonProperty("id") final String id,
		@JsonProperty("searchStrategy") final ModelCheckingSearchStrategy searchStrategy,
		@JsonProperty("nodesLimit") final Integer nodesLimit,
		@JsonProperty("timeLimit") final Integer timeLimit,
		@JsonProperty("options") final Set<ModelCheckingOptions.Options> options,
		@JsonProperty("goal") final String goal
	) {
		super(id);
		this.searchStrategy = Objects.requireNonNull(searchStrategy);
		this.nodesLimit = nodesLimit;
		this.timeLimit = timeLimit;
		this.options = Objects.requireNonNull(options, "options");
		this.goal = goal;
	}

	@Override
	public ValidationTaskType<ProBModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.MODEL_CHECKING;
	}

	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.modelchecking.type.prob");
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

	public ModelCheckingSearchStrategy getSearchStrategy() {
		return this.searchStrategy;
	}

	public Integer getNodesLimit() {
		return nodesLimit;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		final StringJoiner s = new StringJoiner(", ");
		final String strategyKey = ProBModelCheckingTab.getSearchStrategyNameKey(this.getSearchStrategy());
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
	public Trace getTrace() {
		return this.getSteps().isEmpty() ? null : this.getSteps().get(0).getTrace();
	}

	@Override
	public void execute(final ExecutionContext context) {
		// The options must be calculated before adding the ModelCheckingStep,
		// so that the recheckExisting/INSPECT_EXISTING_NODES option is set correctly,
		// which depends on whether any steps were already added.
		ModelCheckingOptions fullOptions = this.getFullOptions(context.stateSpace().getModel());

		int stepIndex = getSteps().size();
		ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, context.stateSpace());
		Platform.runLater(() -> this.getSteps().add(initialStep));
		
		IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				// Command must be executed outside of Platform.runLater to avoid blocking the UI thread!
				var cmd = new GetStatisticsCommand(GetStatisticsCommand.StatisticsOption.MEMORY_USED);
				context.stateSpace().execute(cmd);
				ModelCheckingStep step = new ModelCheckingStep(result, timeElapsed, stats, cmd.getResult(), context.stateSpace());
				setCurrentStep(step);
				Platform.runLater(() -> {
					if (stepIndex < getSteps().size()) {
						getSteps().set(stepIndex, step);
					}
				});
			}

			@Override
			public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				this.updateStats(jobId, timeElapsed, result, stats);
			}
		};
		ConsistencyChecker checker = new ConsistencyChecker(context.stateSpace(), fullOptions, listener);
		
		try {
			this.setCurrentStep(initialStep);
			checker.call();
		} finally {
			this.setCurrentStep(null);
		}
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ProBModelCheckingItem that
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
