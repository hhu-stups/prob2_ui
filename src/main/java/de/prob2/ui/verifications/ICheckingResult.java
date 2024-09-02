package de.prob2.ui.verifications;

import java.util.Collections;
import java.util.List;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface ICheckingResult {
	CheckingStatus getStatus();
	
	default String getMessageBundleKey() {
		return this.getStatus().getTranslationKey();
	}
	
	default Object[] getMessageParams() {
		return new Object[0];
	}
	
	default List<Trace> getTraces() {
		return Collections.emptyList();
	}
	
	default Trace getTrace() {
		return this.getTraces().isEmpty() ? null : this.getTraces().get(0);
	}
	
	default ICheckingResult withoutAnimatorDependentState() {
		return new CheckingResult(this.getStatus(), this.getMessageBundleKey(), this.getMessageParams());
	}
	
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
