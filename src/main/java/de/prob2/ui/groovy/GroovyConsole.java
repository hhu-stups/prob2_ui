package de.prob2.ui.groovy;

import java.io.IOException;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;


public class GroovyConsole extends AnchorPane {
	
	private AnimationSelector animations;
	private int charCounterInLine = 0;
	
	@FXML
	private TextArea tagroovy;
	
	@Inject
	private GroovyConsole(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		try {
			loader.setLocation(getClass().getResource("groovy_console.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tagroovy.setOnMouseClicked(e-> {
			TextArea textArea = (TextArea) e.getSource();
			System.out.println();
		});
		tagroovy.setOnKeyPressed(e-> {
			if(e.getCode().equals(KeyCode.ENTER)) {
				charCounterInLine = 0;
				return;
			}
			if(e.getCode().equals(KeyCode.BACK_SPACE)) {
				if(charCounterInLine == 0) {
					String data = tagroovy.getText();
					tagroovy.setText(data);
				} else {
					charCounterInLine--;
				}
				return;
			}
			tagroovy.selectEnd();
			charCounterInLine++;
		});
	}

}
