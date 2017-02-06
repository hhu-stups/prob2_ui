package de.prob2.ui.beditor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.web.WebEngine;

public class BEditorStage extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorStage.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");
	
	@FXML
	private WebView beditor;
	
	private BEditorSupporter beditorSupporter;
	
	private Path path;
	
	private WebEngine engine;
	
	private boolean loaded = false;
		
	@Inject
	public BEditorStage(final StageManager stageManager, final BEditorSupporter beditorSupporter) {
		stageManager.loadFXML(this, "beditor.fxml");
		engine = beditor.getEngine();
		engine.load(getClass().getResource("beditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
		this.beditorSupporter = beditorSupporter;
		beditorSupporter.setEngine(engine);
	}
	
	@FXML
	private void initialize() {
		this.showingProperty().addListener((observable, from, to) -> {
			if (to) {
				beditorSupporter.startHighlighting();
			} else {
				beditorSupporter.stopHighlighting();
			}
		});
	}
	
	@FXML
	public void handleSave() {
		try {
			Files.write(path, beditorSupporter.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("File not found", e);
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
				Files.write(newFile.toPath(), beditorSupporter.getText().getBytes(EDITOR_CHARSET), option);
				this.setTitle(newFile.getName());
				path = newFile.toPath();
			} catch (IOException e) {
				LOGGER.error("File not found", e);
			}
		}
	}
	
	@FXML
	public void handleClose() {
		this.close();
	}
	
	public void setTextEditor(String editor, Path path) {
		if(engine == null) {
			return;
		}
		this.loaded = true;
		this.path = path;
		String jscallCode = "editor.setValue('" + editor.replaceAll("\\n", "\\\\n") + "')";
		engine.executeScript(jscallCode);
	}
	
	public WebEngine getEngine() {
		return engine;
	}
	
	public boolean getLoaded() {
		return loaded;
	}
}
