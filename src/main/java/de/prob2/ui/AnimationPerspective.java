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

	@SuppressWarnings("unused")
	private final Api api;

	@SuppressWarnings("unused")
	private FXMLLoader loader;

	@Inject
	public AnimationPerspective(Api api, FXMLLoader loader) {
		this.api = api;
		this.loader = loader;

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}

}
