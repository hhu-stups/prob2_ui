package de.prob2.ui.internal;

import de.prob.animator.command.AbstractGetDynamicCommands;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

import java.util.ResourceBundle;

public class DynamicCommandStage extends Stage {
	
	private final class DynamicCommandTraceListener implements ChangeListener<Object> {

		@Override
		public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
			int index = lvChoice.getSelectionModel().getSelectedIndex();
			fillCommands();
			if (index == -1) {
				lvChoice.getSelectionModel().selectFirst();
			} else {
				lvChoice.getSelectionModel().select(index);
			}
		}
		
	}
	
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
	protected Label lbAvailable;

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
	
	public DynamicCommandStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
	}
	
	
	@FXML
	protected void initialize() {
		fillCommands();
		lvChoice.getSelectionModel().selectFirst();
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
		
		currentTrace.currentStateProperty().addListener(new DynamicCommandTraceListener());
		currentTrace.addListener(new DynamicCommandTraceListener());
		currentTrace.stateSpaceProperty().addListener(new DynamicCommandTraceListener());
		
		
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
