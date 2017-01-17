package de.prob2.ui.beditor;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BEditorStage extends Stage {
	
	private static final Logger logger = LoggerFactory.getLogger(BEditorStage.class);
	
	@FXML
	private BEditor beditor;
	
	private Path path;
	
	@Inject
	public BEditorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "beditor.fxml");
	}
	
	public void setTextEditor(String text, Path path) {
		this.path = path;
		beditor.clear();
		beditor.appendText(text);
		beditor.getStyleClass().add("editor");
	}
	
	@FXML
	public void handleSave() {
		try {
			Files.write(path, beditor.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
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
		if(openFile != null) {
			File newFile;
			if(openFile.getAbsolutePath().contains(".")) {
				newFile = new File(openFile.getAbsolutePath());
			} else {
				newFile = new File(openFile.getAbsolutePath() + ".mch");
			}
			StandardOpenOption option = StandardOpenOption.CREATE;
			if(newFile.exists()) {
				option = StandardOpenOption.TRUNCATE_EXISTING;
			}
			try {
				Files.write(newFile.toPath(), beditor.getText().getBytes(), option);
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
