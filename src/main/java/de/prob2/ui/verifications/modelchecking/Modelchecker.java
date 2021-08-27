package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.StateSpaceStats;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.FormalismType;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
		this.currentTasks = new SimpleSetProperty<>(this, "currentTasks", FXCollections.observableSet(new CopyOnWriteArraySet<>()));
		this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Model Checker"));
		stopActions.add(this.executor::shutdownNow);
	}

	public void checkItem(ModelCheckingItem item, boolean recheckExisting, boolean checkAll) {
		if(!item.selected()) {
			return;
		}

		final FutureTask<Void> task = new FutureTask<Void>(() -> {
			startModelchecking(item, recheckExisting, checkAll);
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

	private void startModelchecking(ModelCheckingItem item, boolean recheckExisting, boolean checkAll) {
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
				final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(item.getItems().size() + 1, result, timeElapsed, stats, stateSpace);
				if (!checkAll && jobItem.getResult() instanceof ITraceDescription) {
					currentTrace.set(jobItem.getTrace());
				}
				Platform.runLater(() -> showResult(item, jobItem));
			}
		};
		IModelCheckJob job = buildModelCheckJob(stateSpace, item, recheckExisting, listener);

		try {
			job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
		}
	}

	private IEvalElement getGoal(ModelCheckingItem item) {
		IEvalElement evalElement = null;
		if(currentTrace.getModel().getFormalismType() == FormalismType.B) {
			if(!item.getGoal().equals("-")) {
				AbstractModel model = currentTrace.getModel();
				if(model instanceof EventBModel) {
					evalElement = new EventB(item.getGoal(), FormulaExpand.EXPAND);
				} else {
					evalElement = new ClassicalB(item.getGoal(), FormulaExpand.EXPAND);
				}
			}
		}
		return evalElement;
	}

	private IModelCheckJob buildModelCheckJob(StateSpace stateSpace, ModelCheckingItem item, boolean recheckExisting, IModelCheckListener listener) {
		ConsistencyChecker checker = new ConsistencyChecker(stateSpace, item.getOptions().recheckExisting(recheckExisting), getGoal(item), listener);
		if (!"-".equals(item.getNodesLimit())) {
			checker.getLimitConfiguration().setNodesLimit(Integer.parseInt(item.getNodesLimit()));
		}
		if (!"-".equals(item.getTimeLimit())) {
			checker.getLimitConfiguration().setTimeLimit(Integer.parseInt(item.getTimeLimit()));
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
