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
	
	/**
	 * Get the trace from this result, if any.
	 * If this result contains multiple traces, only the first one is returned
	 * (use {@link #getTraces()} to access the other traces).
	 * If this result doesn't contain a trace, {@code null} is returned.
	 * Note that this says nothing about the result status -
	 * depending on the task type, a trace may be a successful or a failed result.
	 *
	 * @return the first trace from this result, or {@code null} if there is none
	 */
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
