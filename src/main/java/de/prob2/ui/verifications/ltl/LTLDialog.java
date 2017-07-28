package de.prob2.ui.verifications.ltl;


import java.net.URISyntaxException;

import de.prob2.ui.ProB2;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import netscape.javascript.JSObject;

public abstract class LTLDialog extends Dialog<AbstractCheckableItem> {

	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private WebView taCode;
	
	protected WebEngine engine;

			
	public LTLDialog(Class<? extends AbstractCheckableItem> clazz) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				final JSObject editor = (JSObject) engine.executeScript("editor");
				String code = editor.call("getValue").toString();
				if(clazz == LTLPatternItem.class) {
					return new LTLPatternItem(tfName.getText(), taDescription.getText(), code);	
				}
				return new LTLFormulaItem(tfName.getText(), taDescription.getText(), code);	
			}
		});
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
		
	public void setData(String name, String description, String code) {
		tfName.setText(name);
		taDescription.setText(description);
		setTextEditor(code);
	}
	
	public void clear() {
		this.tfName.clear();
		this.taDescription.clear();
	}

	public WebEngine getEngine() {
		return engine;
	}
		
}
