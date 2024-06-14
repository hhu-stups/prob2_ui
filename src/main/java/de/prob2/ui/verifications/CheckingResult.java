package de.prob2.ui.verifications;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

public class CheckingResult {
	private final CheckingStatus status;
	private final String messageBundleKey;
	private final Object[] messageParams;
	
	public CheckingResult(CheckingStatus status, String messageBundleKey, Object... messageParams) {
		this.status = status;
		this.messageBundleKey = messageBundleKey;
		this.messageParams = messageParams;
	}
	
	public CheckingStatus getStatus() {
		return status;
	}
	
	public String getMessageBundleKey() {
		return messageBundleKey;
	}
	
	public Object[] getMessageParams() {
		return messageParams;
	}
	
	public void showAlert(final StageManager stageManager, final I18n i18n) {
		Alert alert = stageManager.makeAlert(
			this.getStatus().equals(CheckingStatus.SUCCESS) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
			this.getStatus().getTranslationKey(),
			this.getMessageBundleKey(), this.getMessageParams());
		alert.setTitle(i18n.translate(this.getStatus()));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
}
