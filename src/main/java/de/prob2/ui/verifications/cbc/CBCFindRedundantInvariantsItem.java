package de.prob2.ui.verifications.cbc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CBCFindRedundantInvariantsItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCFindRedundantInvariantsItem.class);
	
	@JsonCreator
	public CBCFindRedundantInvariantsItem(@JsonProperty("id") String id) {
		super(id);
	}
	
	@Override
	public ValidationTaskType<CBCFindRedundantInvariantsItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_FIND_REDUNDANT_INVARIANTS;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.findRedundantInvariants");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return "";
	}
	
	@Override
	public void execute(ExecutionContext context) {
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("CBC find redundant invariants interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}
		
		List<String> result = cmd.getRedundantInvariants();
		if (result.isEmpty()) {
			this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.cbc.findRedundantInvariants.result.notFound"));
		} else {
			this.setResult(new CheckingResult(cmd.isTimeout() ? CheckingStatus.TIMEOUT : CheckingStatus.FAIL, "verifications.cbc.findRedundantInvariants.result.found", String.join("\n", result)));
		}
	}
}
