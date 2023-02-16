package de.prob2.ui.sharedviews;

import java.util.Objects;

import de.prob2.ui.internal.I18n;
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
	private final I18n i18n;
	
	private Runnable onClose;
	
	public DescriptionView(final Describable describable, final StageManager stageManager, final I18n i18n) {
		this.describable = describable;
		this.i18n = i18n;
		stageManager.loadFXML(this, "description_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(i18n.translate("project.machines.machineDescriptionView.title", describable.getName()));
		String description = describable.getDescription();
		if(description == null) {
			Platform.runLater(this::closeDescriptionView);
			return;
		}
		descriptionText.setText(description.isEmpty() ? i18n.translate("project.machines.machineDescriptionView.placeholder") : describable.getDescription());
		saveButton.visibleProperty().bind(descriptionText.editableProperty());
	}

	public void setOnClose(final Runnable onClose) {
		this.onClose = Objects.requireNonNull(onClose, "onClose");
	}

	@FXML
	public void closeDescriptionView() {
		onClose.run();
	}

	@FXML
	public void editDescription() {
		descriptionText.setEditable(true);
		if(descriptionText.getText().equals(i18n.translate("project.machines.machineDescriptionView.placeholder"))) {
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
			descriptionText.setText(i18n.translate("project.machines.machineDescriptionView.placeholder"));
		}
	}

	public Describable getDescribable() {
		return this.describable;
	}
}
