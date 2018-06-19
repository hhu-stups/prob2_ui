package de.prob2.ui.menu;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@Singleton
public final class AboutBox extends Stage {
	private final ResourceBundle bundle;
	private final VersionInfo versionInfo;

	@FXML
	private Label uiInfoLabel;
	@FXML
	private Label kernelInfoLabel;
	@FXML
	private Label cliInfoLabel;
	@FXML
	private Label javaInfoLabel;

	@Inject
	private AboutBox(final StageManager stageManager, final ResourceBundle bundle, final VersionInfo versionInfo) {
		super();

		this.bundle = bundle;
		this.versionInfo = versionInfo;

		stageManager.loadFXML(this, ""
				+ "_box.fxml");
	}

	@FXML
	private void initialize() {
		this.uiInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.uiInfo"),
			this.versionInfo.getUIBuildTime(),
			this.versionInfo.getUICommit()
		));
		
		this.kernelInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.kernelInfo"),
			this.versionInfo.getKernelVersion(),
			this.versionInfo.getKernelCommit()
		));
		
		this.cliInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.cliInfo"),
			this.bundle.getString("menu.aboutBox.cliInfo.loading"),
			this.bundle.getString("menu.aboutBox.cliInfo.loading")
		));
		new Thread(() -> {
			// The CLI version is loaded in the background, because it requires starting a CLI,
			// which takes a few seconds.
			final String formattedCliVersion = this.versionInfo.getFormattedCliVersion();
			final String revision = this.versionInfo.getCliVersion().revision;
			Platform.runLater(() -> {
				this.cliInfoLabel.setText(
					String.format(this.bundle.getString("menu.aboutBox.cliInfo"),
					formattedCliVersion,
					revision
				));
				this.sizeToScene();
			});
		}, "ProB CLI Version Getter").start();
		
		this.javaInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.javaInfo"),
			System.getProperty("java.version"),
			System.getProperty("java.vendor"),
			System.getProperty("java.vm.name"),
			System.getProperty("java.vm.version"),
			System.getProperty("java.vm.vendor")
		));
	}
}
