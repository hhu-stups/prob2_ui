package de.prob2.ui.verifications;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

public class CheckingResultItem {
	
	private Checked checked;
	private String headerBundleKey;
	private String messageBundleKey;
	private Object[] messageParams;
	
	public CheckingResultItem(Checked checked, String headerBundleKey, String messageBundleKey, Object... messageParams) {
		this.checked = checked;
		this.headerBundleKey = headerBundleKey;
		this.messageBundleKey = messageBundleKey;
		this.messageParams = messageParams;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getHeaderBundleKey() {
		return headerBundleKey;
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
