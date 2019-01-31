package de.prob2.ui.verifications.ltl;


import de.prob2.ui.ProB2;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URISyntaxException;

public abstract class LTLItemStage extends Stage {
	
	@FXML
	private WebView taCode;
	
	@FXML
	private TextArea taDescription;
	
	protected WebEngine engine;

			
	public LTLItemStage() {
		super();
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() throws URISyntaxException {
		engine = taCode.getEngine();
		engine.load(ProB2.class.getClassLoader().getResource("codemirror/LTLEditor.html").toURI().toString());
		engine.setJavaScriptEnabled(true);
	}
	
	private void setTextEditor(String text) {
		final JSObject editor = (JSObject) engine.executeScript("editor");
		editor.call("setValue", text);
	}
		
	public void setData(String description, String code) {
		taDescription.setText(description);
		setTextEditor(code);
	}
	
	public void clear() {
		this.taDescription.clear();
	}

	public WebEngine getEngine() {
		return engine;
	}
		
}
