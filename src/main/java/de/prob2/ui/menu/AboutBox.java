package de.prob2.ui.menu;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AboutBox extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(AboutBox.class);

	private final StageManager stageManager;
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

		this.stageManager = stageManager;
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
		
		String cliVersion;
		String cliRevision;
		try {
			cliVersion = this.versionInfo.getFormattedCliVersion();
			cliRevision = this.versionInfo.getCliVersion().revision;
		} catch (RuntimeException e) {
			LOGGER.error("Failed to start ProB CLI to get version number", e);
			stageManager.makeExceptionAlert(e, "menu.aboutBox.cliStartFailed.message").show();
			cliVersion = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
			cliRevision = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
		}
		this.cliInfoLabel.setText(String.format(
			this.bundle.getString("menu.aboutBox.cliInfo"),
			cliVersion,
			cliRevision
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
