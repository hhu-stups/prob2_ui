package de.prob2.ui.verifications.ltl;


import de.prob2.ui.ProB2;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URISyntaxException;

public abstract class LTLItemStage<T extends ILTLItem> extends Stage {
	
	@FXML
	protected WebView taCode;
	
	@FXML
	protected TextArea taDescription;
	
	@FXML
	protected TextArea taErrors;
	
	protected final CurrentProject currentProject;
	
	protected final ILTLItemHandler ltlItemHandler;
	
	protected final LTLResultHandler resultHandler;
	
	protected LTLHandleItem<T> handleItem;
	
	protected WebEngine engine;

			
	public LTLItemStage(final CurrentProject currentProject, final ILTLItemHandler ltlItemHandler, final LTLResultHandler resultHandler) {
		super();
		this.currentProject = currentProject;
		this.ltlItemHandler = ltlItemHandler;
		this.resultHandler = resultHandler;
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
	
	protected void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	public void setHandleItem(LTLHandleItem<T> handleItem) {
		this.handleItem = handleItem;
	}
	
	protected abstract void addItem(Machine machine, T item);
	
	protected abstract void changeItem(T item, T result);
		
}
