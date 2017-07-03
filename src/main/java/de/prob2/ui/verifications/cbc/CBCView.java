package de.prob2.ui.verifications.cbc;

import java.net.URISyntaxException;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.ltl.MachineTableView;
import de.prob2.ui.verifications.ltl.MachineTableView.CheckingType;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

@Singleton
public class CBCView extends AnchorPane {
	
	@FXML
	private HelpButton helpButton;
	
	@FXML
	private MachineTableView tvMachines;

	@Inject
	public CBCView(final StageManager stageManager) {
		stageManager.loadFXML(this, "cbc_view.fxml");
	}
	
	@FXML
	public void initialize() throws URISyntaxException {
		helpButton.setPathToHelp(ProB2.class.getClassLoader().getResource("help/HelpMain.html").toURI().toString());
		tvMachines.setCheckingType(CheckingType.CBC);
	}
	
	@FXML
	public void addFormula() {
		
	}
		
}
