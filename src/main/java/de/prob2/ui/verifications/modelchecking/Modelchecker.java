package de.prob2.ui.verifications.modelchecking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Modelchecker implements IModelCheckListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);
	private static final AtomicInteger threadCounter = new AtomicInteger(0);
	
	private final Map<String, IModelCheckJob> jobs;
	private final Map<String, ModelCheckingItem> idToItem;
	private final Map<String, ModelCheckStats> idToStats;
	private final ListProperty<Thread> currentJobThreads;
	private final List<IModelCheckJob> currentJobs;
	private final ObjectProperty<IModelCheckingResult> lastResult;
	private final ModelcheckingStage modelcheckingStage;

	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;

	private final Object lock = new Object();
	
	@Inject
	private Modelchecker(final StageManager stageManager, final CurrentTrace currentTrace, 
			final ModelcheckingStage modelcheckingStage, final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.modelcheckingStage = modelcheckingStage;
		this.injector = injector;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		this.currentJobs = new ArrayList<>();
		this.lastResult = new SimpleObjectProperty<>(this, "lastResult", null);
		this.jobs = new HashMap<>();
		this.idToItem = new HashMap<>();
		this.idToStats = new HashMap<>();
	}

	public void checkItem(ModelCheckingItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		
		Thread currentJobThread = new Thread(() -> {
			synchronized(lock) {
				updateCurrentValues(item, currentTrace.getStateSpace());
				startModelchecking(checkAll);
				currentJobThreads.remove(Thread.currentThread());
			}
		}, "Model Check Result Waiter " + threadCounter.getAndIncrement());
		//Adding a new thread for model checking must be done before the thread is started, but it has to be
		//synchronized with other threads accessing the list containing all threads
		synchronized(lock) {
			currentJobThreads.add(currentJobThread);
		}
		currentJobThread.start();
	}
	
	private void updateCurrentValues(ModelCheckingItem item, StateSpace stateSpace) {
		IModelCheckJob job = new ConsistencyChecker(stateSpace, item.getOptions(), null, this);
		idToItem.put(job.getJobId(), item);
		idToStats.put(job.getJobId(), new ModelCheckStats(stageManager, injector));
		currentJobs.add(job);
	}
	
	@Override
	public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		final IModelCheckJob job = jobs.get(jobId);
		if (job == null) {
			throw new IllegalStateException("updateStats was called for an unknown (or already finished) job: " + jobId);
		}
		idToStats.get(jobId).updateStats(job, timeElapsed, stats);
	}
	
	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		final IModelCheckJob job = jobs.remove(jobId);
		if (job == null) {
			throw new IllegalStateException("isFinished was called for an unknown (or already finished) job: " + jobId);
		}
		
		idToStats.get(jobId).isFinished(job, timeElapsed, result);
		Platform.runLater(() -> {
			showResult(jobId, result, job.getStateSpace());
			idToItem.remove(jobId);
			idToStats.remove(jobId);
			modelcheckingStage.hide();
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
		return this.currentJobThreads.emptyProperty().not();
	}
	
	public boolean isRunning() {
		return this.runningProperty().get();
	}
	
	private void startModelchecking(boolean checkAll) {
		modelcheckingStage.setDisableStart(true);
		int size = currentJobs.size();
		IModelCheckJob job = currentJobs.get(size - 1);

		jobs.put(job.getJobId(), job);
		ModelCheckStats currentStats = idToStats.get(job.getJobId());
		currentStats.startJob();
		
		//This must be executed before executing model checking job
		Platform.runLater(() -> injector.getInstance(ModelcheckingView.class).showStats(currentStats));
		
		final IModelCheckingResult result;
		try {
			result = job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
			return;
		} finally {
			modelcheckingStage.setDisableStart(false);
		}
		
		if(!checkAll && result instanceof ITraceDescription) {
			StateSpace s = job.getStateSpace();
			Trace trace = ((ITraceDescription) result).getTrace(s);
			injector.getInstance(CurrentTrace.class).set(trace);
		}
		currentJobs.remove(job);
	}

	public void cancelModelcheck() {
		List<Thread> removedThreads = new ArrayList<>();
		for (Thread thread : currentJobThreads) {
			thread.interrupt();
			removedThreads.add(thread);
		}
		List<IModelCheckJob> removedJobs = new ArrayList<>(currentJobs);
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
		currentJobs.removeAll(removedJobs);
	}
	
	private void showResult(String jobID, IModelCheckingResult result, StateSpace stateSpace) {
		ModelcheckingView modelCheckingView = injector.getInstance(ModelcheckingView.class);
		ModelCheckingItem currentItem = idToItem.get(jobID);
		List<ModelCheckingJobItem> jobItems = currentItem.getItems();
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
		final ModelCheckingJobItem jobItem = new ModelCheckingJobItem(jobItems.size() + 1, checked, result.getMessage(), idToStats.get(jobID), stateSpace, traceDescription);
		jobItems.add(jobItem);
		modelCheckingView.selectItem(currentItem);
		modelCheckingView.selectJobItem(jobItem);
		
		boolean failed = jobItems.stream()
				.map(ModelCheckingJobItem::getChecked)
				.anyMatch(Checked.FAIL::equals);
		boolean success = !failed && jobItems.stream()
				.map(ModelCheckingJobItem::getChecked)
				.anyMatch(Checked.SUCCESS::equals);
		
		if (success) {
			currentItem.setChecked(Checked.SUCCESS);
		} else if (failed) {
			currentItem.setChecked(Checked.FAIL);
		} else {
			currentItem.setChecked(Checked.TIMEOUT);
		}
	}
}
