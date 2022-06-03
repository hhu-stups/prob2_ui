package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

@Singleton
public class TestCaseGenerationResultHandler extends AbstractResultHandler {
	
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final Injector injector;

	
	@Inject
	public TestCaseGenerationResultHandler(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace, final CurrentProject currentProject, final Injector injector) {
		super(stageManager, i18n);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
	}
	
	private void showCheckingResult(TestCaseGenerationItem item, Checked checked, String headerKey, String msgKey, Object... msgParams) {
		item.setResultItem(new CheckingResultItem(checked, headerKey, msgKey, msgParams));
	}
	
	private void showCheckingResult(TestCaseGenerationItem item, Checked checked, String msgKey) {
		showCheckingResult(item, checked, msgKey, msgKey);
	}
	
	public void handleTestCaseGenerationResult(TestCaseGenerationItem item, Object result) {
		item.getExamples().clear();
		if(!(result instanceof TestCaseGeneratorResult)) {
			showCheckingResult(item, Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound");
			return;
		}
		TestCaseGeneratorResult testCaseGeneratorResult = (TestCaseGeneratorResult) result;
		List<TraceInformationItem> traceInformation = extractTraceInformation(testCaseGeneratorResult);
		List<Trace> traces = extractTraces(testCaseGeneratorResult);
		List<TraceInformationItem> uncoveredOperations = extractUncoveredOperations(testCaseGeneratorResult);

		if(testCaseGeneratorResult.isInterrupted()) {
			showCheckingResult(item, Checked.INTERRUPTED, "animation.resultHandler.testcasegeneration.result.interrupted");
		} else if(traces.isEmpty()) {
			showCheckingResult(item, Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound");
		} else if(!uncoveredOperations.isEmpty()) {
			showCheckingResult(item, Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notAllGenerated");
		} else {
			showCheckingResult(item, Checked.SUCCESS, "animation.resultHandler.testcasegeneration.result.found");
		}
		item.getExamples().addAll(traces);
		item.getTraceInformation().setAll(traceInformation);
		item.getUncoveredOperations().setAll(uncoveredOperations);
	}
	
	private List<TraceInformationItem> extractUncoveredOperations(TestCaseGeneratorResult testCaseGeneratorResult) {
		return testCaseGeneratorResult.getUncoveredTargets().stream()
				.map(target -> new TraceInformationItem(-1, new ArrayList<>(), target.getOperation(), target.getGuardString(), target.getFeasible(), null))
				.collect(Collectors.toList());
	}
	
	private List<Trace> extractTraces(TestCaseGeneratorResult testCaseGeneratorResult) {
		return testCaseGeneratorResult.getTestTraces().stream()
				.map(TestTrace::getTrace)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private List<TraceInformationItem> extractTraceInformation(TestCaseGeneratorResult testCaseGeneratorResult) {
		return testCaseGeneratorResult.getTestTraces().stream()
				.map(trace -> new TraceInformationItem(trace.getDepth(), trace.getTransitionNames(), trace.getTarget().getOperation(), trace.getTarget().getGuardString(), trace.getTarget().getFeasible(), trace.getTrace()))
				.collect(Collectors.toList());
	}

	public void saveTraces(TestCaseGenerationItem item) {
		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		if (currentTrace.get() != null) {
			traceSaver.save(item, currentProject.getCurrentMachine());
		}
	}

}
