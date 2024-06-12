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
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.TemporalCheckingResultItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaChecker;
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
		@JsonProperty("expectedResult") boolean expectedResult
	) {
		super(id, code, description, stateLimit, expectedResult);
	}
	
	@Override
	public ValidationTaskType<LTLFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.LTL;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.temporal.type.ltl");
	}
	
	private void handleFormulaResult(IModelCheckingResult result) {
		assert !(result instanceof LTLError);
		
		if (result instanceof LTLCounterExample) {
			this.setCounterExample(((LTLCounterExample)result).getTraceToLoopEntry());
		} else {
			this.setCounterExample(null);
		}
		
		if (result instanceof LTLOk) {
			if (this.getExpectedResult()) {
				this.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.temporal.result.succeeded.message"));
			} else {
				this.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.temporal.result.counterExampleFound.message"));
			}
		} else if (result instanceof LTLCounterExample) {
			if (this.getExpectedResult()) {
				this.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.temporal.result.counterExampleFound.message"));
			} else {
				this.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.temporal.result.succeeded.example.message"));
			}
		} else if (result instanceof LTLNotYetFinished || result instanceof CheckInterrupted) {
			this.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled LTL checking result type: " + result.getClass());
		}
	}
	
	private void handleFormulaParseErrors(List<ErrorItem> errorMarkers) {
		this.setCounterExample(null);
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		this.setResultItem(new TemporalCheckingResultItem(Checked.INVALID_TASK, errorMarkers, "common.result.message", errorMessage));
	}
	
	@Override
	public void execute(ExecutionContext context) {
		try {
			final LTL formula = LTLFormulaChecker.parseFormula(getCode(), context.machine(), context.stateSpace().getModel());
			final LTLChecker checker = new LTLChecker(context.stateSpace(), formula, null, getStateLimit());
			final IModelCheckingResult result = checker.call();
			if (result instanceof LTLError) {
				handleFormulaParseErrors(((LTLError)result).getErrors());
			} else {
				handleFormulaResult(result);
			}
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(error.getErrors());
		}
	}
}
