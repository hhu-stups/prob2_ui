package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob.animator.command.NoTraceFoundException;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

@Singleton
public class TestCaseGenerationResultHandler {

	private static final String GENERAL_RESULT_MESSAGE = "common.result.message";
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> parseErrors;
	protected ArrayList<Class<?>> interrupted;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final Injector injector;

	
	@Inject
	public TestCaseGenerationResultHandler(final ResourceBundle bundle, final CurrentTrace currentTrace, final CurrentProject currentProject, final StageManager stageManager, final Injector injector) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.injector = injector;
		this.success = new ArrayList<>();
		this.parseErrors = new ArrayList<>();
		this.interrupted = new ArrayList<>();
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.parseErrors.addAll(Arrays.asList(ProBError.class, CliError.class, CheckError.class, EvaluationException.class));
		this.interrupted.addAll(Arrays.asList(NoTraceFoundException.class, NotYetFinished.class, CheckInterrupted.class));
	}
	
	private void showCheckingResult(TestCaseGenerationItem item, Checked checked, String headerKey, String msgKey, Object... msgParams) {
		item.setResultItem(new CheckingResultItem(checked, headerKey, msgKey, msgParams));
		handleItem(item, checked);
	}
	
	private void showCheckingResult(TestCaseGenerationItem item, Checked checked, String msgKey) {
		showCheckingResult(item, checked, msgKey, msgKey);
	}
	
	protected void handleItem(TestCaseGenerationItem item, Checked checked) {
		item.setChecked(checked);
	}
	
	public void handleFormulaResult(TestCaseGenerationItem item, Object result) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(parseErrors.contains(clazz)) {
			handleItem(item, Checked.PARSE_ERROR);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		CheckingResultItem resultItem = handleFormulaResult(result);
		item.setResultItem(resultItem);
	}
	
	public CheckingResultItem handleFormulaResult(Object result) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.result.succeeded.header",
					"animation.symbolic.result.succeeded.message");
		} else if(parseErrors.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
					GENERAL_RESULT_MESSAGE, result);
		} else if(interrupted.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
					GENERAL_RESULT_MESSAGE, ((IModelCheckingResult) result).getMessage());
		}
		return resultItem;
	}

	public void handleTestCaseGenerationResult(TestCaseGenerationItem item, Object result, boolean checkAll) {
		item.getExamples().clear();
		if(parseErrors.contains(result.getClass())) {
			showCheckingResult(item, Checked.PARSE_ERROR, "animation.symbolic.resultHandler.testcasegeneration.result.error");
			return;
		} else if(interrupted.contains(result.getClass())) {
			showCheckingResult(item, Checked.INTERRUPTED, "animation.symbolic.resultHandler.testcasegeneration.result.interrupted");
			return;
		}
		TestCaseGeneratorResult testCaseGeneratorResult = (TestCaseGeneratorResult) result;
		List<TraceInformationItem> traceInformation = extractTraceInformation(testCaseGeneratorResult);
		List<Trace> traces = extractTraces(testCaseGeneratorResult);
		List<TraceInformationItem> uncoveredOperations = extractUncoveredOperations(testCaseGeneratorResult);

		if(testCaseGeneratorResult.isInterrupted()) {
			showCheckingResult(item, Checked.INTERRUPTED, "animation.symbolic.resultHandler.testcasegeneration.result.interrupted");
		} else if(traces.isEmpty()) {
			showCheckingResult(item, Checked.FAIL, "animation.symbolic.resultHandler.testcasegeneration.result.notFound");
		} else if(!uncoveredOperations.isEmpty()) {
			showCheckingResult(item, Checked.FAIL, "animation.symbolic.resultHandler.testcasegeneration.result.notAllGenerated");
		} else {
			showCheckingResult(item, Checked.SUCCESS, "animation.symbolic.resultHandler.testcasegeneration.result.found");
		}
		item.getExamples().addAll(traces);
		if(!item.getExamples().isEmpty()) {
			item.putAdditionalInformation("traceInformation", traceInformation);
		}
		item.putAdditionalInformation("uncoveredOperations", uncoveredOperations);
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
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		stageManager.makeAlert(AlertType.INFORMATION,
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content", bundle.getString(itemType.getKey()))
				.showAndWait();
	}
	
	public void showResult(TestCaseGenerationItem item) {
		CheckingResultItem resultItem = item.getResultItem();
		if(resultItem == null || item.getChecked() == Checked.SUCCESS) {
			return;
		}
		Alert alert = stageManager.makeAlert(
				resultItem.getChecked().equals(Checked.SUCCESS) ? AlertType.INFORMATION : AlertType.ERROR,
				resultItem.getHeaderBundleKey(),
				resultItem.getMessageBundleKey(), resultItem.getMessageParams());
		alert.setTitle(item.getName());
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}

}
