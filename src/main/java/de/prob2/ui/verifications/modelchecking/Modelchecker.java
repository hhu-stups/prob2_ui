package de.prob2.ui.verifications.modelchecking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.modelchecking.ModelcheckingStage.SearchStrategy;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

public class Modelchecker implements IModelCheckListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Modelchecker.class);
	private static final AtomicInteger threadCounter = new AtomicInteger(0);
	
	private final Map<String, IModelCheckJob> jobs;
	private final ListProperty<Thread> currentJobThreads;
	private final List<IModelCheckJob> currentJobs;
	private final ObjectProperty<IModelCheckingResult> lastResult;
	private final ModelcheckingStage modelcheckingStage;
	
	private ModelCheckingOptions currentOptions;
	private ModelCheckStats currentStats;

	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;
	
	private Object lock = new Object();
	
	@Inject
	private Modelchecker(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
						 final ModelcheckingStage modelcheckingStage, final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.modelcheckingStage = modelcheckingStage;
		this.injector = injector;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		this.currentJobs = new ArrayList<>();
		this.lastResult = new SimpleObjectProperty<>(this, "lastResult", null);
		this.jobs = new HashMap<>();
	}

	public void checkItem(ModelCheckingItem item, boolean checkAll) {
		if(!item.shouldExecute()) {
			return;
		}
		
		Thread currentJobThread = new Thread(() -> {
			synchronized(lock) {
				updateCurrentValues(item.getOptions(), currentTrace.getStateSpace(), item);
				startModelchecking(checkAll);
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Model Check Result Waiter " + threadCounter.getAndIncrement());
		currentJobThreads.add(currentJobThread);
		currentJobThread.start();
	}
	
	public void checkItem(ModelCheckingOptions options, StringConverter<SearchStrategy> converter, SearchStrategy strategy) {
		Thread currentJobThread = new Thread(() -> {
			synchronized(lock) {
				updateCurrentValues(options, currentTrace.getStateSpace(), converter, strategy);
				startModelchecking(false);
				currentJobThreads.remove(Thread.currentThread());
			}
		}, "Model Check Result Waiter " + threadCounter.getAndIncrement());
		currentJobThreads.add(currentJobThread);
		currentJobThread.start();
	}
	
	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace, StringConverter<SearchStrategy> converter, SearchStrategy strategy) {
		updateCurrentValues(options, stateSpace);
		ModelCheckingItem modelcheckingItem = new ModelCheckingItem(currentOptions, currentStats, converter.toString(strategy));
		if(!currentProject.getCurrentMachine().getModelcheckingItems().contains(modelcheckingItem)) {
			currentProject.getCurrentMachine().addModelcheckingItem(modelcheckingItem);
		} else {
			modelcheckingItem = getItemIfAlreadyExists(modelcheckingItem);
		}
		currentStats.updateItem(modelcheckingItem);
		injector.getInstance(ModelcheckingView.class).selectLast();
	}
	
	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(injector.getInstance(ModelcheckingView.class), stageManager, injector);
		IModelCheckJob job = new ConsistencyChecker(stateSpace, options, null, this);
		currentJobs.add(job);
	}
	
	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace, ModelCheckingItem item) {
		updateCurrentValues(options, stateSpace);
		currentStats.updateItem(item);
	}
	
	@Override
	public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		try {
			final IModelCheckJob job = jobs.get(jobId);
			if (job == null) {
				LOGGER.error("Model check job for ID {} is missing or null", jobId);
				return;
			}
			currentStats.updateStats(job, timeElapsed, stats);
		} catch (RuntimeException e) {
			LOGGER.error("Exception in updateStats", e);
			Platform.runLater(
					() -> stageManager
							.makeExceptionAlert(e,
									"verifications.modelchecking.modelchecker.alerts.updatingStatsError.content")
							.show());
		}
	}
	
	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		final IModelCheckJob job = jobs.remove(jobId);
		if (job == null) {
			// isFinished was already called for this job
			return;
		}
		currentStats.isFinished(job, timeElapsed, result);
		Platform.runLater(() -> {
			modelcheckingStage.hide();
			injector.getInstance(OperationsView.class).update(currentTrace.get());
			injector.getInstance(StatsView.class).update(currentTrace.get());
			lastResult.set(result);
			injector.getInstance(ModelcheckingView.class).refresh();
		});
	}
	
	public ObjectProperty<IModelCheckingResult> resultProperty() {
		return lastResult;
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
	
	private void startModelchecking(boolean checkAll) {
		modelcheckingStage.setDisableStart(true);
		int size = currentJobs.size();
		IModelCheckJob job = currentJobs.get(size - 1);

		jobs.put(job.getJobId(), job);
		currentStats.startJob();
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
		// The consistency checker sometimes doesn't call isFinished, so
		// we call it manually here with some dummy information.
		// If the checker already called isFinished, this call won't do
		// anything - on the first call, the checker was removed from
		// the jobs map, so the second call returns right away.
		isFinished(job.getJobId(), 0, result, new StateSpaceStats(0, 0, 0));
		if(!checkAll && result instanceof ITraceDescription) {
			StateSpace s = job.getStateSpace();
			Trace trace = ((ITraceDescription) result).getTrace(s);
			injector.getInstance(CurrentTrace.class).set(trace);
		}
		currentJobs.remove(job);
	}
	
	public void cancelModelcheck() {
		List<Thread> removedThreads = new ArrayList<>();
		for(Iterator<Thread> iterator = currentJobThreads.iterator(); iterator.hasNext();) {
			Thread thread = iterator.next();
			thread.interrupt();
			removedThreads.add(thread);
		}
		List<IModelCheckJob> removedJobs = new ArrayList<>();
		for(Iterator<IModelCheckJob> iterator = currentJobs.iterator(); iterator.hasNext();) {
			IModelCheckJob job = iterator.next();
			removedJobs.add(job);
		}
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
		currentJobs.removeAll(removedJobs);
	}
	
	private ModelCheckingItem getItemIfAlreadyExists(ModelCheckingItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		int index = currentMachine.getModelcheckingItems().indexOf(item);
		if(index > -1) {
			item = currentMachine.getModelcheckingItems().get(index);
		}
		return item;
	}
	
	public void setCurrentStats(ModelCheckStats currentStats) {
		this.currentStats = currentStats;
	}
	
}
