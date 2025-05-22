package de.prob2.ui.output;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@Singleton
public final class PrologOutputStage extends Stage {
	private final I18n i18n;
	private final MachineLoader machineLoader;
	private final CurrentTrace currentTrace;
	
	@FXML
	private Label statusLabel;
	@FXML
	private Button interruptButton;
	@FXML
	private Button startStopButton;
	@FXML
	private PrologOutput prologOutput;
	@FXML
	private Button clearButton;

	@Inject
	private PrologOutputStage(final StageManager stageManager, final I18n i18n, final MachineLoader machineLoader, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;

		stageManager.loadFXML(this, "prologOutputStage.fxml", this.getClass().getName());
	}

	@FXML
	private void initialize() {
		machineLoader.currentAnimatorProperty().addListener((o, from, to) -> Platform.runLater(this::updateStatus));
		machineLoader.currentAnimatorStartingProperty().addListener((o, from, to) -> Platform.runLater(this::updateStatus));
		currentTrace.animatorBusyProperty().addListener(o -> this.updateStatus());
		this.updateStatus();
	}

	private void updateStatus() {
		final String status;
		if (machineLoader.currentAnimatorProperty().get() != null) {
			if (currentTrace.isAnimatorBusy()) {
				status = i18n.translate("proBCoreConsole.status.inTransaction");
			} else {
				status = i18n.translate("proBCoreConsole.status.running");
			}
			interruptButton.setDisable(false);
			startStopButton.setDisable(false);
			startStopButton.setText(i18n.translate("proBCoreConsole.control.stop"));
			startStopButton.setOnAction(e -> machineLoader.shutdownSharedAnimator());
		} else {
			if (machineLoader.currentAnimatorStartingProperty().get()) {
				status = i18n.translate("proBCoreConsole.status.starting");
				startStopButton.setDisable(true);
			} else {
				status = i18n.translate("proBCoreConsole.status.notRunning");
				startStopButton.setDisable(false);
			}
			interruptButton.setDisable(true);
			startStopButton.setText(i18n.translate("proBCoreConsole.control.start"));
			startStopButton.setOnAction(e -> new Thread(machineLoader::startSharedAnimator, "Shared Animator Starter").start());
		}
		statusLabel.setText(i18n.translate("proBCoreConsole.status", status));
	}

	@FXML
	private void doInterrupt() {
		machineLoader.currentAnimatorProperty().get().sendInterrupt();
	}

	@FXML
	private void doClear() {
		prologOutput.clear();
	}
}
