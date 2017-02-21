package de.prob2.ui.beditor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BEditorStage extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorStage.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");
	
	@FXML
	private WebView beditor;
		
	private Path path;
	
	private WebEngine engine;
	
	private boolean loaded = false;
		
	@Inject
	public BEditorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "beditor.fxml");
		beditor.setContextMenuEnabled(false);
		engine = beditor.getEngine();
		engine.load(getClass().getResource("beditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
	}
		
	@FXML
	public void handleSave() {
		saveFile(path);
	}
	
	@FXML
	public void handleSaveAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Location");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));
		File openFile = fileChooser.showSaveDialog(this.getOwner());
		if (openFile != null) {
			File newFile = new File(openFile.getAbsolutePath() + (openFile.getName().contains(".") ? "" : ".mch"));
			saveFile(newFile.toPath());
			this.setTitle(newFile.getName());
			path = newFile.toPath();
		}
	}
	
	public void saveFile(Path path) {
		StandardOpenOption option = StandardOpenOption.CREATE;
		if(path.toFile().exists()) {
			option = StandardOpenOption.TRUNCATE_EXISTING;
		}
		try {
			String jscallCode = "editor.getValue()";
			String beditorText = engine.executeScript(jscallCode).toString();
			Files.write(path, beditorText.getBytes(EDITOR_CHARSET), option);
		} catch (IOException e) {
			LOGGER.error("File not found", e);
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
		editor = Matcher.quoteReplacement(editor);
		editor = editor.replaceAll("\\n", "\\\\n");
		String jscallCode = "editor.setValue('" + editor + "')";
		engine.executeScript(jscallCode);
	}
	
	public WebEngine getEngine() {
		return engine;
	}
	
	public boolean getLoaded() {
		return loaded;
	}
}
