package de.prob2.ui.verifications.modelchecking;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Modelchecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);

	private final SetProperty<Future<Void>> currentTasks;
	private final ExecutorService executor;

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final StatsView statsView;

	private final Injector injector;

	@Inject
	private Modelchecker(final StageManager stageManager, final CurrentTrace currentTrace,
						 final StopActions stopActions, final StatsView statsView, final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.statsView = statsView;
		this.injector = injector;
		this.currentTasks = new SimpleSetProperty<>(this, "currentTasks", FXCollections.observableSet(new HashSet<>()));
		this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Model Checker"));
		stopActions.add(this.executor::shutdownNow);
	}

	public void checkItem(ModelCheckingItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}

		final FutureTask<Void> task = new FutureTask<Void>(() -> {
			startModelchecking(item, checkAll);
			return null;
		}) {
			@Override
			protected void done() {
				super.done();
				currentTasks.remove(this);
			}
		};
		this.currentTasks.add(task);
		this.executor.submit(task);
	}

	public BooleanExpression runningProperty() {
		return this.currentTasks.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	private static ModelCheckingJobItem makeJobItem(final int index, final IModelCheckingResult result, final long timeElapsed, final StateSpaceStats stats, final StateSpace stateSpace) {
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
		return new ModelCheckingJobItem(index, checked, result.getMessage(), timeElapsed, stats, stateSpace, traceDescription);
	}

	private void startModelchecking(ModelCheckingItem item, boolean checkAll) {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		final ModelcheckingView modelcheckingView = injector.getInstance(ModelcheckingView.class);
		final IModelCheckListener listener = new IModelCheckListener() {
			@Override
			public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				Platform.runLater(() -> modelcheckingView.showStats(timeElapsed, stats));
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
			}

			@Override
			public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
				Platform.runLater(() -> modelcheckingView.showStats(timeElapsed, stats));
				if (stats != null) {
					statsView.updateSimpleStats(stats);
				}
				final ModelCheckingJobItem jobItem = makeJobItem(item.getItems().size() + 1, result, timeElapsed, stats, stateSpace);
				if (!checkAll && jobItem.getTraceDescription() != null) {
					currentTrace.set(jobItem.getTrace());
				}
				Platform.runLater(() -> {
					showResult(item, jobItem);
					Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
					injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
				});
			}
		};
		IModelCheckJob job = buildModelCheckJob(stateSpace, item, listener);

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
		this.currentTasks.forEach(task -> task.cancel(true));
		currentTrace.getStateSpace().sendInterrupt();
	}

	private void showResult(ModelCheckingItem item, ModelCheckingJobItem jobItem) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		List<ModelCheckingJobItem> jobItems = item.getItems();
		jobItems.add(jobItem);
		modelCheckingView.selectItem(item);
		modelCheckingView.selectJobItem(jobItem);
	}
}
