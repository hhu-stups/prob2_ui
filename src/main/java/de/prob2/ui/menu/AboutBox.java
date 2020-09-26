package de.prob2.ui.menu;

import java.util.ResourceBundle;
import java.util.StringJoiner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
	private TextArea versionInfoTextArea;

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
		final StringJoiner versionInfoBuilder = new StringJoiner("\n\n");
		
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.uiInfo"),
			this.versionInfo.getUIVersion(),
			this.versionInfo.getUICommit()
		));
		
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.kernelInfo"),
			this.versionInfo.getKernelVersion(),
			this.versionInfo.getKernelCommit()
		));
		
		String cliVersion;
		String cliRevision;
		String cliLastChangedDate;
		String cliPrologInfo;
		try {
			cliVersion = this.versionInfo.getCliVersion().getShortVersionString();
			cliRevision = this.versionInfo.getCliVersion().revision;
			cliLastChangedDate = this.versionInfo.getCliLastChangedDate();
			cliPrologInfo = this.versionInfo.getCliPrologInfo();
		} catch (RuntimeException e) {
			LOGGER.error("Failed to start ProB CLI to get version number", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "menu.aboutBox.cliStartFailed.message");
			alert.initOwner(this);
			alert.show();
			cliVersion = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
			cliRevision = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
			cliLastChangedDate = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
			cliPrologInfo = bundle.getString("menu.aboutBox.cliStartFailed.placeholder");
		}
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.cliInfo"),
			cliVersion,
			cliRevision,
			cliLastChangedDate,
			cliPrologInfo
		));
		
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.parserInfo"),
			this.versionInfo.getParserVersion(),
			this.versionInfo.getParserCommit()
		));
		
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.javaInfo"),
			System.getProperty("java.version"),
			System.getProperty("java.vendor"),
			System.getProperty("java.vm.name"),
			System.getProperty("java.vm.version"),
			System.getProperty("java.vm.vendor")
		));
		
		versionInfoBuilder.add(String.format(
			this.bundle.getString("menu.aboutBox.javaFxInfo"),
			System.getProperty("javafx.version"),
			System.getProperty("javafx.runtime.version")
		));
		
		this.versionInfoTextArea.setText(versionInfoBuilder.toString());
	}

	@FXML
	private void copyVersionInfo() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(this.versionInfoTextArea.getText());
		clipboard.setContent(content);
	}
}
