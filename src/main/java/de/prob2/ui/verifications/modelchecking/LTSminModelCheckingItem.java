package de.prob2.ui.verifications.modelchecking;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.check.LTSminModelChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTSminModelCheckingOptions;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.application.Platform;

@JsonPropertyOrder({
	"id",
	"backend",
	"options",
	"selected",
})
public final class LTSminModelCheckingItem extends ModelCheckingItem {

	private final LTSminModelCheckingOptions.Backend backend;

	private final Set<LTSminModelCheckingOptions.Option> options;

	@JsonCreator
	public LTSminModelCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("backend") LTSminModelCheckingOptions.Backend backend,
		@JsonProperty("options") Set<LTSminModelCheckingOptions.Option> options
	) {
		super(id);
		this.backend = Objects.requireNonNull(backend);
		this.options = Objects.requireNonNull(options, "options");
	}

	@Override
	public ValidationTaskType<LTSminModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.LTSMIN_MODEL_CHECKING;
	}

	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.modelchecking.type.ltsmin");
	}

	public LTSminModelCheckingOptions.Backend getBackend() {
		return this.backend;
	}

	public Set<LTSminModelCheckingOptions.Option> getOptions() {
		return this.options;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		final StringJoiner s = new StringJoiner(", ");
		s.add("LTSmin");

		final String backendKey = LTSminModelCheckingTab.getBackendNameKey(this.getBackend());
		if (backendKey != null) {
			s.add(i18n.translate(backendKey));
		} else {
			s.add(Objects.toString(this.getBackend()));
		}
		Set<LTSminModelCheckingOptions.Option> opts = this.getOptions();
		if (!opts.contains(LTSminModelCheckingOptions.Option.NO_INV)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_invariant_violations"));
		}
		if (!opts.contains(LTSminModelCheckingOptions.Option.NO_DEAD)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_deadlocks"));
		}
		for (LTSminModelCheckingOptions.Option opt : opts) {
			if (opt == LTSminModelCheckingOptions.Option.NO_INV || opt == LTSminModelCheckingOptions.Option.NO_DEAD) {
				continue;
			}
			s.add(i18n.translate("verifications.modelchecking.description.ltsminOption." + opt.getPrologName()));
		}
		return s.toString();
	}

	@JsonIgnore
	LTSminModelCheckingOptions getFullOptions() {
		Set<LTSminModelCheckingOptions.Option> opts = this.getOptions();
		return LTSminModelCheckingOptions.DEFAULT
				       .backend(this.getBackend())
				       .checkDeadlocks(!opts.contains(LTSminModelCheckingOptions.Option.NO_DEAD))
				       .checkInvariantViolations(!opts.contains(LTSminModelCheckingOptions.Option.NO_INV))
				       .partialOrderReduction(opts.contains(LTSminModelCheckingOptions.Option.POR));
	}

	@Override
	public void execute(ExecutionContext context) {
		ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting LTSmin model check...", Integer.MAX_VALUE), 0, null, null, context.stateSpace());
		Platform.runLater(() -> this.setResult(new ModelCheckingItem.Result(Collections.singletonList(initialStep))));

		IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				ModelCheckingStep step = new ModelCheckingStep(result, timeElapsed, stats, null, context.stateSpace());
				Platform.runLater(() -> setResult(new ModelCheckingItem.Result(Collections.singletonList(step))));
			}

			@Override
			public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				this.updateStats(jobId, timeElapsed, result, stats);
			}
		};
		LTSminModelChecker checker = new LTSminModelChecker(context.stateSpace(), this.getFullOptions(), listener);
		checker.call();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof LTSminModelCheckingItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getBackend(), that.getBackend())
			       && Objects.equals(this.getOptions(), that.getOptions());
	}

	@Override
	public LTSminModelCheckingItem copy() {
		return new LTSminModelCheckingItem(this.getId(), this.getBackend(), new HashSet<>(this.options));
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getBackend(), this.getOptions());
	}
}
