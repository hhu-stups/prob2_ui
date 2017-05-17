package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.AnimationSelector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;


@Singleton
public class LTLPatternView extends AnchorPane {

	@FXML
	private Button addPatternButton;
	
	@FXML
	private Button checkAllButton;
	
	@FXML
	private TableView<Machine> tvMachines;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> machineStatusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> machineNameColumn;
	
	@FXML
	private TableView<LTLFormulaItem> tvPattern;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> statusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> nameColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> descriptionColumn;
	
	private Injector injector;
	
	private CurrentTrace currentTrace;
	
	private CurrentProject currentProject;
	
	private AnimationSelector animations;
	
	@Inject
	private LTLPatternView(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
					final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.animations = animations;
		stageManager.loadFXML(this, "ltlpattern_view.fxml");
	}
	
	@FXML
	public void addPattern() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLPatternDialog.class).showAndWait().ifPresent(item -> {
			/*machine.addLTLFormula(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));*/
		});
	}
	
	@FXML
	public void checkAll() {
		
	}
	
}
