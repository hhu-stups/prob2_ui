package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ModelcheckingView extends AnchorPane {

	private AnimationSelector animations;

	@Inject
	public ModelcheckingView(AnimationSelector ANIMATIONS, FXMLLoader loader) {
		this.animations = ANIMATIONS;
		try {
			loader.setLocation(getClass().getResource("modelchecking_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @FXML
    void handelModelCheck(ActionEvent event) {

    }

    @FXML
    void handleCancel(ActionEvent event) {
    	Stage stage = (Stage) this.getScene().getWindow();
    	stage.close();
    }

}
