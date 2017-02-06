package de.prob2.ui.beditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import javafx.scene.web.WebEngine;

public class BEditorStage extends Stage {
	
	@FXML
	private WebView beditor;
	
	private Path path;
	
	private WebEngine engine;
	
	private boolean loaded = false;
		
	@Inject
	public BEditorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "beditor.fxml");
		engine = beditor.getEngine();
		engine.load(getClass().getResource("beditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
        JSObject jsobj = (JSObject) engine.executeScript("window");
        jsobj.setMember("blexer", this);
        jsobj.setMember("type", "code");
	}
	
	@FXML
	public void handleSave() {

	}
	
	@FXML
	public void handleSaveAs() {

	}
	
	@FXML
	public void handleClose() {

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
