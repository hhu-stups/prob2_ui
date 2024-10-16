package de.prob2.ui.verifications.cbc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.animator.domainobjects.ClassicalB;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.TraceResult;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"selected",
	"predicate",
})
public final class CBCDeadlockFreedomCheckingItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCDeadlockFreedomCheckingItem.class);
	
	private final String predicate;
	
	@JsonCreator
	public CBCDeadlockFreedomCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("predicate") String predicate
	) {
		super(id);
		
		this.predicate = Objects.requireNonNull(predicate, "predicate");
	}
	
	public String getPredicate() {
		return this.predicate;
	}
	
	@Override
	public ValidationTaskType<CBCDeadlockFreedomCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_DEADLOCK_FREEDOM_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.deadlock");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getPredicate();
	}
	
	@Override
	public void execute(ExecutionContext context) {
		ClassicalB parsedPredicate;
		try {
			parsedPredicate = new ClassicalB(this.getPredicate());
		} catch (ProBError exc) {
			LOGGER.error("Failed to parse additional predicate for CBC deadlock checking", exc);
			this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", exc.getMessage()));
			return;
		}
		IModelCheckingResult result = new CBCDeadlockChecker(context.stateSpace(), parsedPredicate).call();
		
		if (result instanceof ModelCheckOk) {
			this.setResult(new CheckingResult(CheckingStatus.SUCCESS));
		} else if (result instanceof CBCDeadlockFound deadlock) {
			this.setResult(new TraceResult(CheckingStatus.FAIL, deadlock.getTrace(context.stateSpace()), "verifications.symbolicModelChecking.result.counterExample"));
		} else if (result instanceof NotYetFinished || result instanceof CheckInterrupted) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", result.getMessage()));
		} else if (result instanceof CheckError) {
			this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled CBC deadlock checking result type: " + result.getClass());
		}
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCDeadlockFreedomCheckingItem o
			&& this.getPredicate().equals(o.getPredicate());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("predicate", this.getPredicate())
			.toString();
	}
}
