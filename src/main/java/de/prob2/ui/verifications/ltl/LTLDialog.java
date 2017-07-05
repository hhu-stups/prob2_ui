package de.prob2.ui.verifications.ltl;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

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

public abstract class LTLDialog<T> extends Dialog<AbstractCheckableItem> {

	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private WebView taCode;
	
	protected WebEngine engine;
	
	protected String text;	
	
	public LTLDialog() {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				final JSObject editor = (JSObject) engine.executeScript("editor");
				String code = editor.call("getValue").toString();
				Type superClass = this.getClass().getGenericSuperclass();
				Type genericType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
				if(genericType.getTypeName() == LTLPatternItem.class.getTypeName()) {
					return new LTLPatternItem(tfName.getText(), taDescription.getText(), code);	
				}
				return new LTLFormulaItem(tfName.getText(), taDescription.getText(), code);	
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		engine = taCode.getEngine();
		engine.load(getClass().getResource("../LTLEditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
		engine.documentProperty().addListener(listener -> {
			if(text != null) {
				final JSObject editor = (JSObject) engine.executeScript("editor");
				editor.call("setValue", text);
			}
		});
	}
	
	private void setTextEditor(String text) {
		if(engine == null) {
			return;
		}
		if(this.text == null) {
			this.text = text;
		} else {
			final JSObject editor = (JSObject) engine.executeScript("editor");
			editor.call("setValue", text);
		}
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
	
}
