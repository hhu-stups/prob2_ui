package de.prob2.ui.verifications.cbc;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.MachineTableView;
import de.prob2.ui.verifications.MachineTableView.CheckingType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;

import javax.inject.Inject;

@Singleton
public class CBCView extends AnchorPane {
	
	@FXML
	private HelpButton helpButton;
	
	@FXML
	private MachineTableView tvMachines;
	
	@FXML
	private TableView<CBCFormulaItem> tvFormula;
	
	@FXML
	private Button addFormulaButton;
	
	private CurrentTrace currentTrace;

	private Injector injector;
	
	@Inject
	public CBCView(final StageManager stageManager, final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this, "cbc_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
		tvMachines.setCheckingType(CheckingType.CBC);
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(CBCChoosingStage.class).showAndWait();
	}
	
	public Machine getCurrentMachine() {
		return tvMachines.getSelectionModel().getSelectedItem();
	}
		
}
