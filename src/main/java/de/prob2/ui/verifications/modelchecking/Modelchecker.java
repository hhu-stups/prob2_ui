package de.prob2.ui.verifications.modelchecking;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import de.prob.statespace.Trace;
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

@Singleton
public class Modelchecker implements IModelCheckListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);
	
	// These three variables should only be used by the single task running on the executor.
	// In particular, they should not be used from the JavaFX application thread (listeners, Platform.runLater, etc.).
	private IModelCheckJob currentJob;
	private ModelCheckingItem currentItem;
	private ModelCheckStats currentStats;
	
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
		this.currentJob = null;
		this.currentItem = null;
		this.currentStats = null;
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
	
	@Override
	public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		this.currentStats.updateStats(this.currentJob, timeElapsed, stats);
	}
	
	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		// The current values of these fields are stored in local fields,
		// so that the Platform.runLater call below can safely use them.
		// Accessing the fields directly might not work correctly,
		// because the next checking job might have already started running and overwritten the fields.
		final IModelCheckJob job = this.currentJob;
		final ModelCheckingItem item = this.currentItem;
		final ModelCheckStats modelCheckStats = this.currentStats;
		this.currentJob = null;
		this.currentItem = null;
		this.currentStats = null;
		modelCheckStats.isFinished(job, timeElapsed, result);
		Platform.runLater(() -> {
			showResult(result, job, item, modelCheckStats);
			injector.getInstance(OperationsView.class).update(currentTrace.get());
			injector.getInstance(StatsView.class).update(job.getStateSpace());
			lastResult.set(result);
			injector.getInstance(ModelcheckingView.class).refresh();
		});
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
	
	private void startModelchecking(ModelCheckingItem item, boolean checkAll) {
		IModelCheckJob job = new ConsistencyChecker(currentTrace.getStateSpace(), item.getOptions(), null, this);
		this.currentJob = job;
		this.currentItem = item;
		this.currentStats = new ModelCheckStats(stageManager, injector);

		this.currentStats.startJob();
		
		//This must be executed before executing model checking job
		final ModelCheckStats stats = this.currentStats;
		Platform.runLater(() -> injector.getInstance(ModelcheckingView.class).showStats(stats));
		
		final IModelCheckingResult result;
		try {
			result = job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
			return;
		}
		
		if(!checkAll && result instanceof ITraceDescription) {
			StateSpace s = job.getStateSpace();
			Trace trace = ((ITraceDescription) result).getTrace(s);
			injector.getInstance(CurrentTrace.class).set(trace);
		}
	}

	public void cancelModelcheck() {
		final Future<?> future = this.currentFuture.get();
		if (future != null) {
			future.cancel(true);
		}
		currentTrace.getStateSpace().sendInterrupt();
	}
	
	private void showResult(IModelCheckingResult result, IModelCheckJob job, ModelCheckingItem item, ModelCheckStats stats) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		List<ModelCheckingJobItem> jobItems = item.getItems();
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
			traceDescription = (ITraceDescription) result;
		} else {
			traceDescription = null;
		}
		final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(jobItems.size() + 1, checked, result.getMessage(), stats, job.getStateSpace(), traceDescription);
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
	}
}
