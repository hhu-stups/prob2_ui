package de.prob2.ui.internal;

import java.util.ResourceBundle;


import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DynamicCommandStage extends Stage {
	
	@FXML
	protected ListView<DynamicCommandItem> lvChoice;

	@FXML
	protected TextArea taFormula;

	@FXML
	protected VBox enterFormulaBox;

	@FXML
	protected Label lbDescription;

	@FXML
	protected Label lbAvailable;

	@FXML
	protected CheckBox cbContinuous;

	@FXML
	protected ScrollPane pane;
	
	protected DynamicCommandItem currentItem;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ResourceBundle bundle;
	
	protected final StageManager stageManager;
	
	protected Thread currentThread;
	
	public DynamicCommandStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.stageManager = stageManager;
	}
	
	
	@FXML
	protected void initialize() {
		lvChoice.getSelectionModel().selectFirst();
		fillCommands();
		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			if (!to.isAvailable()) {
				lbAvailable.setText(String.join("\n", bundle.getString("tableview.notavailable"), to.getAvailable()));
			} else {
				lbAvailable.setText("");
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			lbDescription.setText(to.getDescription());
			String currentFormula = taFormula.getText();
			if ((!needFormula || !currentFormula.isEmpty()) && (currentItem == null
					|| !currentItem.getCommand().equals(to.getCommand()) || cbContinuous.isSelected())) {
				reset();
				visualize(to);
				currentItem = to;
			}
		});
		
		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			int index = lvChoice.getSelectionModel().getSelectedIndex();
			fillCommands();
			if (index == -1) {
				return;
			}
			lvChoice.getSelectionModel().select(index);
		});
		
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			fillCommands();
			reset();
		});
		
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
					if (item == null) {
						return;
					}
					visualize(item);
					e.consume();
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			}
		});
	}
	
	@FXML
	private void cancel() {
		interrupt();
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	protected void interrupt(){
		if (currentThread != null) {
			currentThread.interrupt();
		}
	};
	
	protected void reset(){}
	
	protected void visualize(DynamicCommandItem item){}
	
	protected void fillCommands(){}

}
