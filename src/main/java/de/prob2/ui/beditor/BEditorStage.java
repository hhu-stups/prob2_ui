package de.prob2.ui.beditor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BEditorStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(BEditorStage.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");
	
	@FXML
	private BEditor beditor;
	
	private Path path;
	
	@Inject
	public BEditorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "beditor.fxml");
	}
	
	@FXML
	private void initialize() {
		this.showingProperty().addListener((observable, from, to) -> {
			if (to) {
				beditor.startHighlighting();
			} else {
				beditor.stopHighlighting();
			}
		});
	}
	
	public void setEditorText(String text, Path path) {
		this.path = path;
		beditor.replaceText(text);
	}
	
	@FXML
	public void handleSave() {
		try {
			Files.write(path, beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.error("File not found", e);
		}
	}
	
	@FXML
	public void handleSaveAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Location");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));
		File openFile = fileChooser.showSaveDialog(this.getOwner());
		if (openFile != null) {
			File newFile = new File(openFile.getAbsolutePath() + (openFile.getName().contains(".") ? "" : ".mch"));
			StandardOpenOption option = StandardOpenOption.CREATE;
			if(newFile.exists()) {
				option = StandardOpenOption.TRUNCATE_EXISTING;
			}
			try {
				Files.write(newFile.toPath(), beditor.getText().getBytes(EDITOR_CHARSET), option);
				this.setTitle(newFile.getName());
				path = newFile.toPath();
			} catch (IOException e) {
				logger.error("File not found", e);
			}
		}
	}
	
	@FXML
	public void handleClose() {
		this.close();
	}
}
