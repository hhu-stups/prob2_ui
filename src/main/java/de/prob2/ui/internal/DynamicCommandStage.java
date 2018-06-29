package de.prob2.ui.internal;

import de.prob.animator.command.AbstractGetDynamicCommands;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.modelchecking.ModelcheckingView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import java.util.ResourceBundle;

public class DynamicCommandStage extends Stage {

	
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCommandStage.class);
	
	@FXML
	protected ListView<DynamicCommandItem> lvChoice;

	@FXML
	protected TextArea taFormula;

	@FXML
	protected VBox enterFormulaBox;

	@FXML
	protected Label lbDescription;

	@FXML
	protected CheckBox cbContinuous;

	@FXML
	protected ScrollPane pane;
	
	@FXML
	protected Button cancelButton;
	
	@FXML
	protected DynamicCommandStatusBar statusBar;
	
	protected DynamicCommandItem currentItem;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final ResourceBundle bundle;
	
	protected final StageManager stageManager;
	
	protected final ObjectProperty<Thread> currentThread;
	
	protected final Injector injector;
	
	public DynamicCommandStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
	}
	
	
	@FXML
	protected void initialize() {
		fillCommands();
		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			String currentFormula = taFormula.getText();
			if(currentItem != null && !currentItem.getCommand().equals(to.getCommand())) {
				reset();
			}
			if ((!needFormula || !currentFormula.isEmpty()) && (currentItem == null
					|| !currentItem.getCommand().equals(to.getCommand()) || cbContinuous.isSelected())) {
				visualize(to);
			}
			if(from != null) {
				currentItem = to;
			}
		});
		lvChoice.disableProperty().bind(currentThread.isNotNull());
		
		currentTrace.currentStateProperty().addListener((observable, from, to) -> refresh());
		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.stateSpaceProperty().addListener((observable, from, to) -> refresh());
		injector.getInstance(ModelcheckingView.class).resultProperty().addListener((observable, from, to) -> refresh());
		
		
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
		lvChoice.setCellFactory(item -> new DynamicCommandItemCell());
		cancelButton.disableProperty().bind(currentThread.isNull());
	}
	
	protected void fillCommands(AbstractGetDynamicCommands cmd) {
		try {
			lvChoice.getItems().clear();
			currentTrace.getStateSpace().execute(cmd);
			for (DynamicCommandItem item : cmd.getCommands()) {
				lvChoice.getItems().add(item);
			}
		} catch (Exception e) {
			LOGGER.error("Extract all expression table commands failed", e);
		}
	}
	
	@FXML
	protected void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		interrupt();
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	private void refresh() {
		int index = lvChoice.getSelectionModel().getSelectedIndex();
		fillCommands();
		if (index == -1) {
			lvChoice.getSelectionModel().selectFirst();
		} else {
			lvChoice.getSelectionModel().select(index);
		}	
	}
	
	protected void interrupt(){
		if (currentThread.get() != null) {
			currentThread.get().interrupt();
			currentThread.set(null);
		}
		reset();
	}
	
	protected void reset(){
		throw new UnsupportedOperationException();
	}
	
	protected void visualize(DynamicCommandItem item){
		throw new UnsupportedOperationException();
	}
	
	protected void fillCommands(){
		throw new UnsupportedOperationException();
	}

}
