package de.prob2.ui;

import java.net.URL;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

public class AnimationPerspective implements Initializable {

	private final Api api;

	@FXML
	Button foo;

	private FXMLLoader loader;

	@Inject
	public AnimationPerspective(Api api, FXMLLoader loader) {
		this.api = api;
		this.loader = loader;

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		foo.setText(api.getVersion().toString());
		foo.setOnAction(e -> {
			System.out.println("proc");
		});
	}

}
