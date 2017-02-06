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

@Deprecated
public class OldBEditorStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(OldBEditorStage.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");
	
	@FXML
	private OldBEditor beditor;
	
	private Path path;
	
	@Inject
	public OldBEditorStage(final StageManager stageManager) {
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
