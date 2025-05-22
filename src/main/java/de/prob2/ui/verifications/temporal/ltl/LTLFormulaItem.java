package de.prob2.ui.verifications.temporal.ltl;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLChecker;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ErrorsResult;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.TraceResult;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaParser;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LTLFormulaItem extends TemporalFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(LTLFormulaItem.class);
	
	@JsonCreator
	public LTLFormulaItem(
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
	public ValidationTaskType<LTLFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.LTL;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.temporal.type.ltl");
	}

	@Override
	public LTLFormulaItem copy() {
		return new LTLFormulaItem(this.getId(), this.getCode(), this.getDescription(), this.getStateLimit(), this.getStartState(), this.getStartStateExpression(), this.getExpectedResult());
	}

	private void handleFormulaResult(IModelCheckingResult result) {
		assert !(result instanceof LTLError);
		
		if (result instanceof LTLOk) {
			if (this.getExpectedResult()) {
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS));
			} else {
				this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.temporal.result.counterExampleFound.message"));
			}
		} else if (result instanceof LTLCounterExample counterExample) {
			Trace trace = counterExample.getTraceToLoopEntry();
			if (this.getExpectedResult()) {
				this.setResult(new TraceResult(CheckingStatus.FAIL, trace, "verifications.temporal.result.counterExampleFound.message"));
			} else {
				this.setResult(new TraceResult(CheckingStatus.SUCCESS, trace, "verifications.temporal.result.succeeded.example.message"));
			}
		} else if (result instanceof LTLNotYetFinished || result instanceof CheckInterrupted) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled LTL checking result type: " + result.getClass());
		}
	}
	
	private void handleFormulaParseErrors(List<ErrorItem> errorMarkers) {
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		this.setResult(new ErrorsResult(CheckingStatus.INVALID_TASK, errorMarkers, "common.result.message", errorMessage));
	}
	
	@Override
	protected void execute(ExecutionContext context, State startState) {
		LTL formula;
		try {
			formula = LTLFormulaParser.parseFormula(getCode(), context.machine(), context.stateSpace().getModel());
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(error.getErrors());
			return;
		}
		
		LTLChecker checker = new LTLChecker(context.stateSpace(), formula, null, getStateLimit(), startState);
		IModelCheckingResult result = checker.call();
		if (result instanceof LTLError) {
			handleFormulaParseErrors(((LTLError)result).getErrors());
		} else {
			handleFormulaResult(result);
		}
	}
}
