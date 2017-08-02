package de.prob2.ui.beditor;

import com.google.inject.Inject;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class BEditorStage extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorStage.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");
	
	@FXML private MenuBar menuBar;
	@FXML private WebView beditor;
	
	private final StageManager stageManager;
	
	private Path path;
	private WebEngine engine;
	
	@Inject
	public BEditorStage(final StageManager stageManager) {
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "beditor.fxml");
	}
	
	@FXML
	private void initialize() throws URISyntaxException {
		this.stageManager.setMacMenuBar(this, this.menuBar);
		beditor.setContextMenuEnabled(false);
		engine = beditor.getEngine();
		engine.load(ProB2.class.getClassLoader().getResource("codemirror/beditor.html").toURI().toString());
		engine.setJavaScriptEnabled(true);
		new BTokenProvider(engine);

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
		try {
			final JSObject editor = (JSObject)engine.executeScript("editor");
			String beditorText = (String)editor.call("getValue");
			Files.write(path, beditorText.getBytes(EDITOR_CHARSET));
		} catch (IOException e) {
			LOGGER.error("Could not save file", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not save file:\n" + e).showAndWait();
		}
	}
	
	@FXML
	public void handleClose() {
		this.close();
	}
	
	public void setTextEditor(String text, Path path) {
		if (engine == null) {
			return;
		}
		this.path = path;
		final JSObject editor = (JSObject) engine.executeScript("editor");
		editor.call("setValue", text);
	}


	
	public WebEngine getEngine() {
		return engine;
	}

}
