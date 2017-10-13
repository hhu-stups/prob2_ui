package de.prob2.ui.verifications.symbolicchecking;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

public class SymbolicCheckingChoosingStage extends Stage {
	
	@FXML
	private RadioButton rbInvariant;
	
	@FXML
	private RadioButton rbSequence;
	
	@FXML
	private RadioButton rbDeadlock;
	
	
	private final Injector injector;
	
	@Inject
	private SymbolicCheckingChoosingStage(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void choose() {
		if(rbInvariant.isSelected()) {
			injector.getInstance(SymbolicCheckingInvariants.class).showAndWait();
		} else if(rbSequence.isSelected()) {
			injector.getInstance(SymbolicCheckingSequence.class).showAndWait();
		} else {
			injector.getInstance(SymbolicCheckingDeadlock.class).showAndWait();
		}
		this.close();
	}

}
