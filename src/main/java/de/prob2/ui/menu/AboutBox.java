package de.prob2.ui.menu;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.cli.CliVersionNumber;
import de.prob.scripting.Api;

import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AboutBox extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(AboutBox.class);
	
	private final Api api;
	private final ResourceBundle bundle;
	private final Properties buildInfo;
	private String kernelVersion;
	private String kernelCommit;
	
	@FXML private Label uiInfoLabel;
	@FXML private Label kernelInfoLabel;
	@FXML private Label cliInfoLabel;
	@FXML private Label javaInfoLabel;
	
	@Inject
	private AboutBox(final StageManager stageManager, final Api api, final ResourceBundle bundle) {
		super();
		
		this.api = api;
		this.bundle = bundle;
		
		this.buildInfo = new Properties();
		try (final InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("/de/prob2/ui/build.properties"), Charset.forName("UTF-8"))) {
			this.buildInfo.load(reader);
		} catch (IOException e) {
			LOGGER.error("Failed to load build info", e);
		}
		
		try {
			this.kernelVersion = Main.getVersion();
			this.kernelCommit = Main.getGitSha();
		} catch (IOException e) {
			LOGGER.error("Failed to get kernel version and commit", e);
			final String message = String.format("I/O error, see log: %s", e);
			if (this.kernelVersion == null) {
				this.kernelVersion = message;
			}
			if (this.kernelCommit == null) {
				this.kernelCommit = message;
			}
		}
		
		stageManager.loadFXML(this, "about_box.fxml");
	}
	
	@FXML
	private void initialize() {
		this.uiInfoLabel.setText(String.format(
			this.bundle.getString("about.uiInfo"),
			this.buildInfo.getProperty("buildTime"),
			this.buildInfo.getProperty("commit")
		));
		
		this.kernelInfoLabel.setText(String.format(
			this.bundle.getString("about.kernelInfo"),
			this.kernelVersion,
			this.kernelCommit
		));
		
		// noinspection RedundantStringFormatCall
		this.cliInfoLabel.setText(String.format(this.bundle.getString("about.cliInfoLoading")));
		new Thread(() -> {
			final CliVersionNumber cliVersion = this.api.getVersion();
			Platform.runLater(() -> {
				this.cliInfoLabel.setText(String.format(
					this.bundle.getString("about.cliInfo"),
					cliVersion.major,
					cliVersion.minor,
					cliVersion.service,
					cliVersion.qualifier,
					cliVersion.revision
				));
				this.sizeToScene();
			});
		}, "ProB CLI Version Getter").start();
		
		this.javaInfoLabel.setText(String.format(
			this.bundle.getString("about.javaInfo"),
			System.getProperty("java.version"),
			System.getProperty("java.vendor"),
			System.getProperty("java.vm.name"),
			System.getProperty("java.vm.version"),
			System.getProperty("java.vm.vendor")
		));
	}
}
