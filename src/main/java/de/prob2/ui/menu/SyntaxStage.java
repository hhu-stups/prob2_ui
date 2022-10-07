package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@FXMLInjected
public final class SyntaxStage extends Stage {
	@FXML
	private TextArea syntaxText;

	private static final Logger LOGGER = LoggerFactory.getLogger(SyntaxStage.class);
	private final StageManager stageManager;

	@Inject
	private SyntaxStage (StageManager stageManager) {
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "syntaxStage.fxml");
	}
	void setContent(Path path) {
		String text;
		try (final Stream<String> lines = Files.lines(path)) {
			text = lines.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file: {}", path, e);
			final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotReadFile.content", path);
			alert.initOwner(this.getScene().getWindow());
			alert.show();
			return;
		}
		syntaxText.setText(text);
	}
}
