package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

@Singleton
public class TestCaseGenerationResultHandler {
	@Inject
	public TestCaseGenerationResultHandler() {}
	
	public void handleTestCaseGenerationResult(TestCaseGenerationItem item, Object result) {
		item.getExamples().clear();
		if(!(result instanceof TestCaseGeneratorResult)) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound"));
			return;
		}
		TestCaseGeneratorResult testCaseGeneratorResult = (TestCaseGeneratorResult) result;
		List<TraceInformationItem> traceInformation = extractTraceInformation(testCaseGeneratorResult);
		List<Trace> traces = extractTraces(testCaseGeneratorResult);
		List<TraceInformationItem> uncoveredOperations = extractUncoveredOperations(testCaseGeneratorResult);

		if(testCaseGeneratorResult.isInterrupted()) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.resultHandler.testcasegeneration.result.interrupted"));
		} else if(traces.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound"));
		} else if(!uncoveredOperations.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notAllGenerated"));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.resultHandler.testcasegeneration.result.found"));
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
}
