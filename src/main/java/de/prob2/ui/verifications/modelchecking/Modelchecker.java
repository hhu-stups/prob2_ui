package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Singleton
public class Modelchecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);

	private final ObjectProperty<Future<?>> currentFuture;
	private final ExecutorService executor;
	private final ObjectProperty<IModelCheckingResult> lastResult;

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	@Inject
	private Modelchecker(final StageManager stageManager, final CurrentTrace currentTrace,
						 final StopActions stopActions, final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.currentFuture = new SimpleObjectProperty<>(this, "currentFuture", null);
		this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Model Checker"));
		stopActions.add(this.executor::shutdownNow);
		this.lastResult = new SimpleObjectProperty<>(this, "lastResult", null);
	}

	public void checkItem(ModelCheckingItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}

		this.currentFuture.set(this.executor.submit(() -> {
			try {
				startModelchecking(item, checkAll);
			} finally {
				Platform.runLater(() -> this.currentFuture.set(null));
			}
		}));
	}

	public ObjectProperty<IModelCheckingResult> resultProperty() {
		return lastResult;
	}

	public BooleanExpression runningProperty() {
		return this.currentFuture.isNotNull();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	private static ModelCheckingJobItem makeJobItem(final int index, final IModelCheckingResult result, final ModelCheckStats stats, final StateSpace stateSpace) {
		final Checked checked;
		if (result instanceof ModelCheckOk || result instanceof LTLOk) {
			checked = Checked.SUCCESS;
		} else if (result instanceof ITraceDescription) {
			checked = Checked.FAIL;
		} else {
			checked = Checked.TIMEOUT;
		}
		final ITraceDescription traceDescription;
		if (result instanceof ITraceDescription) {
			traceDescription = (ITraceDescription)result;
		} else {
			traceDescription = null;
		}
		return new ModelCheckingJobItem(index, checked, result.getMessage(), stats, stateSpace, traceDescription);
	}

	private void startModelchecking(ModelCheckingItem item, boolean checkAll) {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		final ModelCheckStats modelCheckStats = new ModelCheckStats(stageManager, injector);
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				modelCheckStats.updateStats(stateSpace, timeElapsed, stats);
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				modelCheckStats.isFinished(stateSpace, timeElapsed, result);
				final ModelCheckingJobItem jobItem = makeJobItem(item.getItems().size() + 1, result, modelCheckStats, stateSpace);
				if (!checkAll && jobItem.getTraceDescription() != null) {
					currentTrace.set(jobItem.getTrace());
				}
				Platform.runLater(() -> {
					showResult(item, jobItem);
					lastResult.set(result);
					injector.getInstance(ModelcheckingView.class).refresh();
				});
			}
		};
		IModelCheckJob job = buildModelCheckJob(stateSpace, item, listener);

		modelCheckStats.startJob();

		//This must be executed before executing model checking job
		Platform.runLater(() -> injector.getInstance(ModelcheckingView.class).showStats(modelCheckStats));

		try {
			job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
		}
	}

	private IModelCheckJob buildModelCheckJob(StateSpace stateSpace, ModelCheckingItem item, IModelCheckListener listener) {
		ConsistencyChecker checker = new ConsistencyChecker(stateSpace, item.getOptions(), null, listener);
		if (!"-".equals(item.getNodesLimit())) {
			checker.setNodesLimit(Integer.parseInt(item.getNodesLimit()));
		}
		return checker;
	}

	public void cancelModelcheck() {
		final Future<?> future = this.currentFuture.get();
		if (future != null) {
			future.cancel(true);
		}
		currentTrace.getStateSpace().sendInterrupt();
	}

	private void showResult(ModelCheckingItem item, ModelCheckingJobItem jobItem) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		List<ModelCheckingJobItem> jobItems = item.getItems();
		jobItems.add(jobItem);
		modelCheckingView.selectItem(item);
		modelCheckingView.selectJobItem(jobItem);

		boolean failed = jobItems.stream()
				.map(ModelCheckingJobItem::getChecked)
				.anyMatch(Checked.FAIL::equals);
		boolean success = !failed && jobItems.stream()
				.map(ModelCheckingJobItem::getChecked)
				.anyMatch(Checked.SUCCESS::equals);

		if (success) {
			item.setChecked(Checked.SUCCESS);
		} else if (failed) {
			item.setChecked(Checked.FAIL);
		} else {
			item.setChecked(Checked.TIMEOUT);
		}

		modelCheckingView.refresh();
	}
}
