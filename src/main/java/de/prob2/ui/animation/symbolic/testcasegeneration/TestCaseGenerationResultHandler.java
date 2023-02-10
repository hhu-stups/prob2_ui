package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

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
		
		List<Trace> traces = new ArrayList<>();
		for (final TestTrace trace : testCaseGeneratorResult.getTestTraces()) {
			if (trace.getTrace() != null) {
				traces.add(trace.getTrace());
			}
		}
		
		if(testCaseGeneratorResult.isInterrupted()) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.resultHandler.testcasegeneration.result.interrupted"));
		} else if(traces.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound"));
		} else if(!testCaseGeneratorResult.getUncoveredTargets().isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notAllGenerated"));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.resultHandler.testcasegeneration.result.found"));
		}
		item.getExamples().addAll(traces);
		item.getTraceInformation().setAll(testCaseGeneratorResult.getTestTraces());
		item.getUncoveredOperations().setAll(testCaseGeneratorResult.getUncoveredTargets());
	}
}
