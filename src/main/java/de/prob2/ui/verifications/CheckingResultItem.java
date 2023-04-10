package de.prob2.ui.verifications;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

public class CheckingResultItem {
	
	private Checked checked;
	private String messageBundleKey;
	private Object[] messageParams;
	
	public CheckingResultItem(Checked checked, String messageBundleKey, Object... messageParams) {
		this.checked = checked;
		this.messageBundleKey = messageBundleKey;
		this.messageParams = messageParams;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getHeaderBundleKey() {
		switch (this.getChecked()) {
			case NOT_CHECKED: return "verifications.result.notChecked.header";
			case SUCCESS: return "verifications.result.succeeded.header";
			case FAIL: return "verifications.result.failed.header";
			case TIMEOUT: return "verifications.symbolicchecking.resultHandler.symbolicChecking.result.timeout";
			case INTERRUPTED: return "common.result.interrupted.header";
			case PARSE_ERROR: return "common.result.couldNotParseFormula.header";
			case LIMIT_REACHED: return "verifications.symbolicchecking.resultHandler.symbolicChecking.result.limitReached";
			default: throw new IllegalArgumentException("Unhandled checked status: " + this.getChecked());
		}
	}
	
	public String getMessageBundleKey() {
		return messageBundleKey;
	}
	
	public Object[] getMessageParams() {
		return messageParams;
	}
	
	public void showAlert(final StageManager stageManager, final I18n i18n) {
		Alert alert = stageManager.makeAlert(
			this.getChecked().equals(Checked.SUCCESS) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
			this.getHeaderBundleKey(),
			this.getMessageBundleKey(), this.getMessageParams());
		alert.setTitle(i18n.translate(this.getHeaderBundleKey()));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
}
