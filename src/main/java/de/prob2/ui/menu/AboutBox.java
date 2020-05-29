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
	private Label parserInfoLabel;
	@FXML
	private Label javaInfoLabel;
	@FXML
	private Label javaFxInfoLabel;

	@Inject
	private AboutBox(final StageManager stageManager, final ResourceBundle bundle, final VersionInfo versionInfo) {
		super();

		this.bundle = bundle;
		this.versionInfo = versionInfo;

		stageManager.loadFXML(this, "about_box.fxml");
	}

	@FXML
	private void initialize() {
		this.uiInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.uiInfo"),
			this.versionInfo.getUIVersion(),
			this.versionInfo.getUICommit()
		));
		
		this.kernelInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.kernelInfo"),
			this.versionInfo.getKernelVersion(),
			this.versionInfo.getKernelCommit()
		));
		
		this.cliInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.cliInfo"),
			this.versionInfo.getFormattedCliVersion(),
			this.versionInfo.getCliVersion().revision
		));
		
		this.parserInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.parserInfo"),
			this.versionInfo.getParserVersion(),
			this.versionInfo.getParserCommit()
		));
		
		this.javaInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.javaInfo"),
			System.getProperty("java.version"),
			System.getProperty("java.vendor"),
			System.getProperty("java.vm.name"),
			System.getProperty("java.vm.version"),
			System.getProperty("java.vm.vendor")
		));
		
		this.javaFxInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.javaFxInfo"),
			System.getProperty("javafx.version"),
			System.getProperty("javafx.runtime.version")
		));
	}
}
