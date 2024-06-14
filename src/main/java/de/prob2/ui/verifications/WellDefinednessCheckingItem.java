package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WellDefinednessCheckingItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(WellDefinednessCheckingItem.class);
	
	@JsonCreator
	public WellDefinednessCheckingItem(@JsonProperty("id") String id) {
		super(id);
	}
	
	@Override
	public ValidationTaskType<WellDefinednessCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.WELL_DEFINEDNESS_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.wellDefinednessChecking");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return "";
	}
	
	@Override
	public void execute(ExecutionContext context) {
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Well-definedness checking interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}

		if (cmd.getDischargedCount().equals(cmd.getTotalCount())) {
			this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.allDischarged.message", cmd.getTotalCount()));
		} else {
			this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.symbolicchecking.resultHandler.wellDefinednessChecking.result.undischarged.message", cmd.getDischargedCount(), cmd.getTotalCount(), cmd.getTotalCount().subtract(cmd.getDischargedCount())));
		}
	}
}
