package de.prob2.ui.verifications;

import de.prob.check.IModelCheckingResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
	
	private static final String GENERAL_RESULT_MESSAGE = "common.result.message";
	
	protected final StageManager stageManager;
	protected final ResourceBundle bundle;
	
	protected CheckingType type;
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> counterExample;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> interrupted;
	protected ArrayList<Class<?>> parseErrors;
	
	protected AbstractResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		success = new ArrayList<>();
		counterExample = new ArrayList<>();
		error = new ArrayList<>();
		interrupted = new ArrayList<>();
		parseErrors = new ArrayList<>();
	}
	
	public void showResult(AbstractCheckableItem item) {
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
	
	public CheckingResultItem handleFormulaResult(Object result, State stateid, List<Trace> traces) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.SUCCESS, "verifications.result.succeeded.header",
					"verifications.result.succeeded.message", bundle.getString(type.getKey()));
		} else if(counterExample.contains(result.getClass())) {
			traces.addAll(handleCounterExample(result, stateid));
			resultItem = new CheckingResultItem(Checked.FAIL, "verifications.result.counterExampleFound.header",
					"verifications.result.counterExampleFound.message", bundle.getString(type.getKey()));
		} else if(error.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.FAIL, "common.result.error.header",
					GENERAL_RESULT_MESSAGE, ((IModelCheckingResult) result).getMessage());
		} else if(result instanceof Throwable || parseErrors.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
					GENERAL_RESULT_MESSAGE, result);
		} else if(interrupted.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
					GENERAL_RESULT_MESSAGE, ((IModelCheckingResult) result).getMessage());
		}
		return resultItem;
	}
	
	protected abstract List<Trace> handleCounterExample(Object result, State stateid);
	
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		stageManager.makeAlert(AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content", bundle.getString(itemType.getKey()))
				.showAndWait();
	}
	
	protected void handleItem(AbstractCheckableItem item, Checked checked) {
		item.setChecked(checked);
		Platform.runLater(() -> {
			if (checked == Checked.SUCCESS) {
				item.setCheckedSuccessful();
			} else if (checked == Checked.FAIL) {
				item.setCheckedFailed();
			} else if (checked == Checked.INTERRUPTED || checked == Checked.TIMEOUT) {
				item.setCheckInterrupted();
			} else if (checked == Checked.PARSE_ERROR) {
				item.setParseError();
			}
		});
	}

}
