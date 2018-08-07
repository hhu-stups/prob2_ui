package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

@Singleton
public class SymbolicAnimationResultHandler {
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> interrupted;
	
	@Inject
	public SymbolicAnimationResultHandler(final ResourceBundle bundle, final CurrentTrace currentTrace) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.success = new ArrayList<>();
		this.error = new ArrayList<>();
		this.interrupted = new ArrayList<>();
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.interrupted.addAll(Arrays.asList(NotYetFinished.class, CheckInterrupted.class));
	}

	public void handleFindValidState(SymbolicAnimationFormulaItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.symbolic.findValidState.result.found"), Checked.SUCCESS);
			item.setExample(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.symbolic.findValidState.result.notFound"), Checked.FAIL);
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, bundle.getString("verifications.symbolic.findValidState.result.interrupted"), Checked.INTERRUPTED);
		} else {
			showCheckingResult(item, bundle.getString("verifications.symbolic.findValidState.result.error"), Checked.FAIL);
		}
	}
	
	public void handleFindRedundantInvariants(SymbolicAnimationFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if(cmd.isInterrupted()) {
			showCheckingResult(item, bundle.getString("verifications.interrupted"), Checked.INTERRUPTED);
		} else if (result.isEmpty()) {
			showCheckingResult(item, bundle.getString("verifications.symbolic.findRedundantInvariants.result.notFound"), Checked.SUCCESS);
		} else {
			final String header = bundle.getString(cmd.isTimeout() ? "verifications.symbolic.findRedundantInvariants.result.timeout" : "verifications.symbolic.findRedundantInvariants.result.found");
			showCheckingResult(item, String.join("\n", result), header, Checked.FAIL);
		}
	}
	
	private void showCheckingResult(SymbolicAnimationFormulaItem item, String msg, String header, Checked checked) {
		Alert.AlertType alertType = checked == Checked.SUCCESS ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
		item.setResultItem(new CheckingResultItem(alertType , checked, msg, header));
		handleItem(item, checked);
	}
	
	private void showCheckingResult(SymbolicAnimationFormulaItem item, String msg, Checked checked) {
		showCheckingResult(item, msg, msg, checked);
	}
	
	protected void handleItem(AbstractCheckableItem item, Checked checked) {
		item.setChecked(checked);
		if(checked == Checked.SUCCESS) {
			item.setCheckedSuccessful();
		} else if(checked == Checked.FAIL) {
			item.setCheckedFailed();
		} else if(checked == Checked.INTERRUPTED || checked == Checked.TIMEOUT) {
			item.setCheckInterrupted();
		}
	}
	
	public void handleFormulaResult(SymbolicAnimationFormulaItem item, Object result, State stateid) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(error.contains(clazz) || result instanceof Throwable) {
			handleItem(item, Checked.FAIL);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		CheckingResultItem resultItem = handleFormulaResult(result, stateid);
		item.setResultItem(resultItem);
	}
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Alert.AlertType.INFORMATION, Checked.SUCCESS, String.format(bundle.getString("verifications.result.succeeded"), bundle.getString("verifications.itemType.formula")), "Success");
		} else if(error.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Alert.AlertType.ERROR, Checked.FAIL, ((IModelCheckingResult) result).getMessage(), bundle.getString("verifications.result.error"));
		} else if(result instanceof Throwable) {
			resultItem = new CheckingResultItem(Alert.AlertType.ERROR, Checked.FAIL, bundle.getString("verifications.result.couldNotParseFormula.message") + " " + result, bundle.getString("verifications.result.couldNotParseFormula.header"));
		} else if(interrupted.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Alert.AlertType.ERROR, Checked.INTERRUPTED, ((IModelCheckingResult) result).getMessage(),  bundle.getString("verifications.interrupted"));
		}
		return resultItem;
	}
	
	public void handleFormulaResult(SymbolicAnimationFormulaItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicAnimationType.FIND_VALID_STATE) {
			handleFindValidState(item, (FindStateCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicAnimationType.FIND_REDUNDANT_INVARIANTS) {
			handleFindRedundantInvariants(item, (GetRedundantInvariantsCommand) cmd);
		}
	}
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(String.format("%s already exists", bundle.getString(itemType.getKey())));
		alert.setHeaderText(String.format("%s already exists", bundle.getString(itemType.getKey())));
		alert.setContentText(String.format("Declared %s already exists", bundle.getString(itemType.getKey())));
		alert.showAndWait();
	}
	
	public void showResult(SymbolicAnimationFormulaItem item) {
		CheckingResultItem resultItem = item.getResultItem();
		if(resultItem == null || item.getChecked() == Checked.SUCCESS) {
			return;
		}
		Alert alert = new Alert(resultItem.getType(), resultItem.getMessage());
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.getHeader());
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}

}
