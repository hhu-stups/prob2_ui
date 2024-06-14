package de.prob2.ui.verifications;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

public interface ICheckingResult {
	CheckingStatus getStatus();
	String getMessageBundleKey();
	Object[] getMessageParams();
	
	default void showAlert(final StageManager stageManager, final I18n i18n) {
		Alert alert = stageManager.makeAlert(
			this.getStatus().equals(CheckingStatus.SUCCESS) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
			this.getStatus().getTranslationKey(),
			this.getMessageBundleKey(), this.getMessageParams());
		alert.setTitle(i18n.translate(this.getStatus()));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
}
