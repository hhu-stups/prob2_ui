package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.check.CBCInvariantChecker;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.TraceResult;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"id",
	"selected",
	"operationName",
})
public final class CBCInvariantPreservationCheckingItem extends SymbolicCheckingFormulaItem {
	private final String operationName;
	
	@JsonCreator
	public CBCInvariantPreservationCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("operationName") String operationName
	) {
		super(id);
		
		this.operationName = operationName;
	}
	
	public String getOperationName() {
		return this.operationName;
	}
	
	@Override
	public ValidationTaskType<CBCInvariantPreservationCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_INVARIANT_PRESERVATION_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.invariant");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		if (this.getOperationName() == null) {
			return i18n.translate("verifications.symbolicchecking.choice.checkAllOperations");
		} else {
			return this.getOperationName();
		}
	}
	
	@Override
	public void execute(ExecutionContext context) {
		ArrayList<String> eventNames;
		if (getOperationName() == null) {
			// Check all operations/events
			eventNames = null;
		} else {
			// Check only one specific operation/event
			eventNames = new ArrayList<>();
			eventNames.add(getOperationName());
		}
		IModelCheckingResult result = new CBCInvariantChecker(context.stateSpace(), eventNames).call();
		
		if (result instanceof ModelCheckOk) {
			this.setResult(new CheckingResult(CheckingStatus.SUCCESS));
		} else if (result instanceof CBCInvariantViolationFound violation) {
			List<Trace> counterExamples = new ArrayList<>();
			int size = violation.getCounterexamples().size();
			for (int i = 0; i < size; i++) {
				counterExamples.add(violation.getTrace(i, context.stateSpace()));
			}
			this.setResult(new TraceResult(CheckingStatus.FAIL, counterExamples, "verifications.symbolicModelChecking.result.counterExample"));
		} else if (result instanceof NotYetFinished || result instanceof CheckInterrupted) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", result.getMessage()));
		} else if (result instanceof CheckError) {
			this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled CBC invariant checking result type: " + result.getClass());
		}
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCInvariantPreservationCheckingItem o
			&& Objects.equals(this.getOperationName(), o.getOperationName());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("operationName", this.getOperationName())
			.toString();
	}
}
