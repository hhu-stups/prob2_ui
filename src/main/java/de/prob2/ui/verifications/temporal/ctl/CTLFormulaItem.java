package de.prob2.ui.verifications.temporal.ctl;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.animator.domainobjects.CTL;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.CTLChecker;
import de.prob.check.CTLCouldNotDecide;
import de.prob.check.CTLCounterExample;
import de.prob.check.CTLError;
import de.prob.check.CTLNotYetFinished;
import de.prob.check.CTLOk;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.TemporalCheckingResult;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CTLFormulaItem extends TemporalFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CTLFormulaItem.class);
	
	@JsonCreator
	public CTLFormulaItem(
		@JsonProperty("id") String id,
		@JsonProperty("code") String code,
		@JsonProperty("description") String description,
		@JsonProperty("stateLimit") int stateLimit,
		@JsonProperty("startState") StartState startState,
		@JsonProperty("startStateExpression") String startStateExpression,
		@JsonProperty("expectedResult") boolean expectedResult
	) {
		super(id, code, description, stateLimit, startState, startStateExpression, expectedResult);
	}
	
	@Override
	public ValidationTaskType<CTLFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.CTL;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.temporal.type.ctl");
	}
	
	private void handleFormulaResult(IModelCheckingResult result) {
		assert !(result instanceof CTLError);
		
		if (result instanceof CTLCounterExample) {
			this.setCounterExample(((CTLCounterExample) result).getTrace());
		}
		
		if (result instanceof CTLOk) {
			if (this.getExpectedResult()) {
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.temporal.result.succeeded.message"));
			} else {
				this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.temporal.result.counterExampleFound.message"));
			}
		} else if (result instanceof CTLCounterExample) {
			if (this.getExpectedResult()) {
				this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.temporal.result.counterExampleFound.message"));
			} else {
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.temporal.result.succeeded.example.message"));
			}
		} else if (result instanceof CTLNotYetFinished || result instanceof CheckInterrupted || result instanceof CTLCouldNotDecide) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled CTL checking result type: " + result.getClass());
		}
	}
	
	private void handleFormulaParseErrors(List<ErrorItem> errorMarkers) {
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		this.setResult(new TemporalCheckingResult(CheckingStatus.INVALID_TASK, errorMarkers, "common.result.message", errorMessage));
	}
	
	@Override
	protected void execute(ExecutionContext context, State startState) {
		CTL formula;
		try {
			formula = CTLFormulaParser.parseFormula(getCode(), context.stateSpace().getModel());
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(error.getErrors());
			return;
		}
		
		CTLChecker checker = new CTLChecker(context.stateSpace(), formula, null, getStateLimit(), startState);
		IModelCheckingResult result = checker.call();
		if (result instanceof CTLError) {
			handleFormulaParseErrors(((CTLError) result).getErrors());
		} else {
			handleFormulaResult(result);
		}
	}
}
