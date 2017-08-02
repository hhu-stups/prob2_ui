package de.prob2.ui;

import com.google.inject.Inject;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class GenericListController {

	@FXML
	ListView<String> list;

	@FXML
	BorderPane genericListContainer;

	private Stage stage;

	@Inject
	public GenericListController(FXMLLoader loader, Stage stage) {
		this.stage = stage;
		try {
			loader.setLocation(getClass().getResource("generic_list.fxml"));
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stage.setScene(new Scene(genericListContainer, 450, 450));
	}

	public void setContent(List<String> content) {
		ObservableList<String> items = list.getItems();
		items.clear();
		items.addAll(content);
	}

	public void setTitle(String title) {
		stage.setTitle(title);
	}

	public void show() {
		stage.showAndWait();
	}

}
