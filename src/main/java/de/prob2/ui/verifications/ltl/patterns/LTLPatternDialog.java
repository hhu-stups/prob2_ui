package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import netscape.javascript.JSObject;

public class LTLPatternDialog extends Dialog<LTLPatternItem> {
	
	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private WebView taPattern;
	
	private WebEngine engine;
	
	private String text;
	
	@Inject
	public LTLPatternDialog(final StageManager stageManager, final Injector injector, final CurrentProject currentProject) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				final JSObject editor = (JSObject) engine.executeScript("editor");
				String pattern = editor.call("getValue").toString();
				return new LTLPatternItem(tfName.getText(), taDescription.getText(), pattern);
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlpattern_dialog.fxml");
	}
	
	@FXML
	public void initialize() {
		engine = taPattern.getEngine();
		engine.load(getClass().getResource("../LTLEditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
		engine.documentProperty().addListener(listener -> {
			final JSObject editor = (JSObject) engine.executeScript("editor");
			editor.call("setValue", text);
		});
	}
	
	private void setTextEditor(String text) {
		if(engine == null) {
			return;
		}
		this.text = text;
	}
	
	public void setData(String name, String description, String formula) {
		tfName.setText(name);
		taDescription.setText(description);
		setTextEditor(formula);
	}
	
	public void clear() {
		this.tfName.clear();
		this.taDescription.clear();
	}

}
