package de.prob2.ui.verifications.modelchecking;

import com.fasterxml.jackson.annotation.*;
import de.prob.check.*;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.*;
import javafx.application.Platform;
import de.prob.check.TLCModelChecker;
import de.tlc4b.TLC4BCliOptions.TLCOption;

import java.math.BigInteger;
import java.util.*;

@JsonPropertyOrder({
	"id",
	"searchStrategy",
	"options",
	"selected",
})
public final class TLCModelCheckingItem extends ModelCheckingItem {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final ModelCheckingSearchStrategy searchStrategy;

	private final Map<TLCOption, String> options;

	@JsonCreator
	public TLCModelCheckingItem(
		@JsonProperty("id") final String id,
		@JsonProperty("searchStrategy") final ModelCheckingSearchStrategy searchStrategy,
		@JsonProperty("options") final Map<TLCOption, String> options
	) {
		super(id);
		this.searchStrategy = Objects.requireNonNull(searchStrategy);
		this.options = Objects.requireNonNull(options, "options");
	}

	public Map<TLCOption, String> getOptions() {
		return this.options;
	}

	public ModelCheckingSearchStrategy getSearchStrategy() {
		return this.searchStrategy;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		final StringJoiner s = new StringJoiner(", ");
		s.add("TLC");

		final String strategyKey = TLCModelCheckingTab.getSearchStrategyNameKey(this.getSearchStrategy());
		if (strategyKey != null) {
			String depth = this.options.containsKey(TLCOption.DFID) ? " (" + this.options.get(TLCOption.DFID) + ")" : "";
			s.add(i18n.translate(strategyKey) + depth);
		} else {
			s.add(this.getSearchStrategy().toString());
		}
		Map<TLCOption, String> opts = this.getOptions();
		if (!opts.containsKey(TLCOption.NODEAD)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_deadlocks"));
		}
		if (!opts.containsKey(TLCOption.NOINV)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_invariant_violations"));
		}
		if (!opts.containsKey(TLCOption.NOASS)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_assertion_violations"));
		}
		if (!opts.containsKey(TLCOption.NOGOAL)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_goal"));
		}
		if (!opts.containsKey(TLCOption.NOLTL)) {
			s.add("Check LTL assertions");//i18n.translate("verifications.modelchecking.description.option.find_goal"));
		}
		for (TLCOption opt : opts.keySet()) {
			if (opt == TLCOption.DFID)
				continue; // already handled by search strategy
			s.add(opt.arg());//i18n.translate("verifications.modelchecking.description.tlcOption." + opt.arg()));
		}
		return s.toString();
	}

	@Override
	public Trace getTrace() {
		return this.getSteps().isEmpty() ? null : this.getSteps().get(0).getTrace();
	}

	@Override
	public void execute(final ExecutionContext context) {
		final int stepIndex = getSteps().size();
		final ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting TLC model check...", Integer.MAX_VALUE), 0, null, BigInteger.ZERO, context.stateSpace());
		getSteps().add(initialStep);

		IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				final ModelCheckingStep step = new ModelCheckingStep(result, timeElapsed, stats, BigInteger.ZERO, context.stateSpace());
				setCurrentStep(step);
				Platform.runLater(() -> {
					if (stepIndex < getSteps().size()) {
						getSteps().set(stepIndex, step);
					}
				});
			}

			@Override
			public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				updateStats(jobId, timeElapsed, result, stats);
			}
		};
		TLCModelChecker tlcModelChecker = new TLCModelChecker(
			context.project().getLocation().resolve(context.machine().getLocation()).toString(),
			context.trace().getStateSpace(),
			listener,
			// this.options already contains the options set by ProB preferences, which have not been overwritten!
			new TLCModelCheckingOptions(context.stateSpace(), getOptions()));

		try {
			setCurrentStep(initialStep);
			tlcModelChecker.call();
		} finally {
			setCurrentStep(null);
		}
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TLCModelCheckingItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getSearchStrategy(), that.getSearchStrategy())
			       && Objects.equals(this.getOptions(), that.getOptions());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getSearchStrategy(), this.getOptions());
	}
}
