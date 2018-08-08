package de.prob2.ui.verifications;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

public abstract class AbstractResultHandler {
	
	public enum ItemType {
		FORMULA("verifications.abstractResultHandler.itemType.formula"),
		PATTERN("verifications.abstractResultHandler.itemType.pattern"),
		;
		
		private final String key;
		
		ItemType(final String key) {
			this.key = key;
		}
		
		public String getKey() {
			return this.key;
		}
	}
	
	protected final StageManager stageManager;
	protected final ResourceBundle bundle;
	
	protected CheckingType type;
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> counterExample;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> interrupted;
	
	protected AbstractResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		success = new ArrayList<>();
		counterExample = new ArrayList<>();
		error = new ArrayList<>();
		interrupted = new ArrayList<>();
	}
	
	public void showResult(AbstractCheckableItem item) {
		CheckingResultItem resultItem = item.getResultItem();
		if(resultItem == null || item.getChecked() == Checked.SUCCESS) {
			return;
		}
		Alert alert = new Alert(
				resultItem.getChecked().equals(Checked.SUCCESS) ? AlertType.INFORMATION : AlertType.ERROR,
				resultItem.getMessage());
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.getHeader());
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid, List<Trace> traces) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.SUCCESS, String.format(bundle.getString("verifications.result.succeeded"), bundle.getString(type.getKey())), "Success");
		} else if(counterExample.contains(result.getClass())) {
			traces.addAll(handleCounterExample(result, stateid));
			resultItem = new CheckingResultItem(Checked.FAIL, String.format(bundle.getString("verifications.result.counterExampleFound"), bundle.getString(type.getKey())), "Counter Example Found");
		} else if(error.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.FAIL, ((IModelCheckingResult) result).getMessage(), bundle.getString("verifications.result.error"));
		} else if(result instanceof Throwable) {
			resultItem = new CheckingResultItem(Checked.FAIL, bundle.getString("verifications.result.couldNotParseFormula.message") + " " + result, bundle.getString("verifications.result.couldNotParseFormula.header"));
		} else if(interrupted.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.INTERRUPTED, ((IModelCheckingResult) result).getMessage(),  bundle.getString("verifications.interrupted"));
		}
		return resultItem;
	}
	
	protected abstract List<Trace> handleCounterExample(Object result, State stateid);
	
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		Alert alert = stageManager.makeAlert(AlertType.INFORMATION,
				String.format(bundle.getString("verifications.abstractResultHandler.alerts.alreadyExists.content"),
						bundle.getString(itemType.getKey())));
		alert.setHeaderText(
				String.format(bundle.getString("verifications.abstractResultHandler.alerts.alreadyExists.header"),
						bundle.getString(itemType.getKey())));
		alert.showAndWait();
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

}
