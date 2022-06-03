package de.prob2.ui.internal;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

public abstract class AbstractResultHandler {
	
	public enum ItemType {
		CONFIGURATION("verifications.abstractResultHandler.itemType.configuration"),
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
	protected final I18n i18n;
	

	
	protected AbstractResultHandler(final StageManager stageManager, final I18n i18n) {
		this.stageManager = stageManager;
		this.i18n = i18n;
	}
	
	public void showResult(AbstractCheckableItem item) {
		CheckingResultItem resultItem = item.getResultItem();
		Alert alert = stageManager.makeAlert(
				resultItem.getChecked().equals(Checked.SUCCESS) ? AlertType.INFORMATION : AlertType.ERROR,
				resultItem.getHeaderBundleKey(),
				resultItem.getMessageBundleKey(), resultItem.getMessageParams());
		alert.setTitle(i18n.translate(resultItem.getHeaderBundleKey()));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
	
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		stageManager.makeAlert(AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content", i18n.translate(itemType.getKey()))
				.show();
	}
}
