package de.prob2.ui.sharedviews;

import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

public class DescriptionView extends AnchorPane {

	public interface Describable {
		String getName();
		String getDescription();
		void setDescription(String description);
	}

	@FXML
	private Label titelLabel;
	@FXML
	private TextArea descriptionText;
	@FXML
	private Button saveButton;

	private final Describable describable;
	private final Runnable closeMethod;
	private final ResourceBundle bundle;
	
	public DescriptionView(final Describable describable, final Runnable closeMethod, final StageManager stageManager, final Injector injector) {
		this.describable = describable;
		this.closeMethod = closeMethod;
		this.bundle = injector.getInstance(ResourceBundle.class);
		stageManager.loadFXML(this, "description_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(String.format(bundle.getString("project.machines.machineDescriptionView.title"), describable.getName()));
		String description = describable.getDescription();
		if(description == null) {
			Platform.runLater(() -> closeDescriptionView());
			return;
		}
		descriptionText.setText(description.isEmpty()? bundle.getString("project.machines.machineDescriptionView.placeholder") : describable.getDescription());
		saveButton.visibleProperty().bind(descriptionText.editableProperty());
	}

	@FXML
	public void closeDescriptionView() {
		closeMethod.run();
	}

	@FXML
	public void editDescription() {
		descriptionText.setEditable(true);
		if(descriptionText.getText().equals(bundle.getString("project.machines.machineDescriptionView.placeholder"))) {
			descriptionText.clear();
		}
		descriptionText.requestFocus();
		descriptionText.positionCaret(descriptionText.getText().length());
	}

	@FXML
	public void saveDescription() {
		descriptionText.setEditable(false);
		describable.setDescription(descriptionText.getText());
		if(descriptionText.getText().isEmpty()) {
			descriptionText.setText(bundle.getString("project.machines.machineDescriptionView.placeholder"));
		}
	}

	public Describable getDescribable() {
		return this.describable;
	}
}
