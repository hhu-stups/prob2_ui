package de.prob2.ui.verifications.modelchecking;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.check.TLCModelChecker;
import de.prob.check.TLCModelCheckingOptions;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.tlc4b.TLC4BOption;

import javafx.application.Platform;

@JsonPropertyOrder({
	"id",
	"searchStrategy",
	"options",
	"selected",
})
public final class TLCModelCheckingItem extends ModelCheckingItem {

	private final ModelCheckingSearchStrategy searchStrategy;

	/**
	 * Keep null values in this map, they are important!
	 */
	private final Map<TLC4BOption, String> options;

	@JsonCreator
	public TLCModelCheckingItem(
		@JsonProperty("id") final String id,
		@JsonProperty("searchStrategy") final ModelCheckingSearchStrategy searchStrategy,
		@JsonProperty("options") final Map<TLC4BOption, String> options
	) {
		super(id);
		this.searchStrategy = Objects.requireNonNull(searchStrategy);
		this.options = Objects.requireNonNull(options, "options");
	}

	@Override
	public ValidationTaskType<TLCModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.TLC_MODEL_CHECKING;
	}

	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.modelchecking.type.tlc");
	}

	public Map<TLC4BOption, String> getOptions() {
		return this.options;
	}

	public ModelCheckingSearchStrategy getSearchStrategy() {
		return this.searchStrategy;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		final StringJoiner s = new StringJoiner(", ");
		s.add("TLC");
		final Map<TLC4BOption, String> opts = this.getOptions();
		if (!opts.containsKey(TLC4BOption.NOTRANSLATION)) {
			s.add(i18n.translate("verifications.modelchecking.description.tlcOption.translation"));
		}
		final String strategyKey = TLCModelCheckingTab.getSearchStrategyNameKey(this.getSearchStrategy());
		if (strategyKey != null) {
			String depth = this.options.containsKey(TLC4BOption.DFID) ? " (" + this.options.get(TLC4BOption.DFID) + ")" : "";
			s.add(i18n.translate(strategyKey) + depth);
		} else {
			s.add(this.getSearchStrategy().toString());
		}
		if (opts.containsKey(TLC4BOption.WORKERS)) {
			s.add(opts.get(TLC4BOption.WORKERS) + " workers");
		}
		if (!opts.containsKey(TLC4BOption.NODEAD)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_deadlocks"));
		}
		if (!opts.containsKey(TLC4BOption.NOINV)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_invariant_violations"));
		}
		if (!opts.containsKey(TLC4BOption.NOASS)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_assertion_violations"));
		}
		if (!opts.containsKey(TLC4BOption.NOGOAL)) {
			s.add(i18n.translate("verifications.modelchecking.description.option.find_goal"));
		}
		if (!opts.containsKey(TLC4BOption.NOLTL)) {
			s.add(i18n.translate("verifications.modelchecking.description.tlcOption.noltl"));
		}
		for (TLC4BOption opt : opts.keySet()) {
			if (opt == TLC4BOption.NOTRANSLATION || opt == TLC4BOption.DFID || opt == TLC4BOption.NODEAD || opt == TLC4BOption.NOINV || opt == TLC4BOption.NOASS
				|| opt == TLC4BOption.NOGOAL || opt == TLC4BOption.NOLTL || opt == TLC4BOption.WORKERS ) {
				continue; // already handled
			}
			if (opt == TLC4BOption.MININT || opt == TLC4BOption.MAXINT || opt == TLC4BOption.TMP || opt == TLC4BOption.OUTPUT) {
				continue; // ignore for now
			}
			s.add(i18n.translate("verifications.modelchecking.description.tlcOption." + opt.arg()));
		}
		return s.toString();
	}

	@Override
	public void execute(final ExecutionContext context) {
		StateSpace stateSpace = context.stateSpace();

		final ModelCheckingStep initialStep = new ModelCheckingStep(new NotYetFinished("Starting TLC model check...", Integer.MAX_VALUE), 0, null, null, stateSpace);
		Platform.runLater(() -> this.setResult(new ModelCheckingItem.Result(Collections.singletonList(initialStep))));

		IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				final ModelCheckingStep step = new ModelCheckingStep(result, timeElapsed, stats, null, stateSpace);
				Platform.runLater(() -> setResult(new ModelCheckingItem.Result(Collections.singletonList(step))));
			}

			@Override
			public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
				updateStats(jobId, timeElapsed, result, stats);
			}
		};

		TLCModelCheckingOptions tlcOptions = new TLCModelCheckingOptions(fixPath(context, this.getOptions()));
		TLCModelChecker tlcModelChecker = new TLCModelChecker(
			TLCModelCheckingTab.getMachinePathForTlc(context.project(), context.machine(), context.stateSpace(), context.i18n(), tlcOptions.getOptions().containsKey(TLC4BOption.NOTRANSLATION)).toString(),
			stateSpace, listener, tlcOptions);
		tlcModelChecker.call();
	}

	private static Map<TLC4BOption, String> fixPath(ExecutionContext context, Map<TLC4BOption, String> options) {
		String pathStr = options.get(TLC4BOption.OUTPUT);
		if (pathStr != null) {
			Path path = Path.of(pathStr);
			Path resolved = context.project().resolveProjectPath(path);
			if (!path.equals(resolved)) {
				HashMap<TLC4BOption, String> fixed = new HashMap<>(options);
				fixed.put(TLC4BOption.OUTPUT, resolved.toString());
				return fixed;
			}
		}
		return options;
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
	public TLCModelCheckingItem copy() {
		return new TLCModelCheckingItem(this.getId(), this.searchStrategy, new HashMap<>(this.options));
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getSearchStrategy(), this.getOptions());
	}
}
