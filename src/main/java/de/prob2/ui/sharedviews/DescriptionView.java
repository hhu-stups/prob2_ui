package de.prob2.ui.sharedviews;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import java.io.IOException;
import java.util.Objects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

public class DescriptionView extends AnchorPane {
	@FXML
	private Label titelLabel;
	@FXML
	private TextArea descriptionText;
	@FXML
	private Button saveButton;

	private final I18n i18n;
	
	private Runnable onClose;
	
	private final StringProperty name;
	private final StringProperty description;
	
	public DescriptionView(final StageManager stageManager, final I18n i18n) {
		this.i18n = i18n;
		
		this.name = new SimpleStringProperty(this, "name", "");
		this.description = new SimpleStringProperty(this, "description", "");
		
		stageManager.loadFXML(this, "description_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.textProperty().bind(i18n.translateBinding("project.machines.machineDescriptionView.title", this.nameProperty()));
		this.descriptionProperty().addListener((o, from, to) -> {
			if (to.isEmpty()) {
				descriptionText.setText(i18n.translate("project.machines.machineDescriptionView.placeholder"));
			} else {
				descriptionText.setText(to);
			}
		});
		saveButton.visibleProperty().bind(descriptionText.editableProperty());
	}

	public void setOnClose(final Runnable onClose) {
		this.onClose = Objects.requireNonNull(onClose, "onClose");
	}

	public StringProperty nameProperty() {
		return this.name;
	}

	public String getName() {
		return this.nameProperty().get();
	}

	public void setName(final String name) {
		this.nameProperty().set(name);
	}

	public StringProperty descriptionProperty() {
		return this.description;
	}

	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public void setDescription(final String description) {
		this.descriptionProperty().set(description);
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
		this.setDescription(descriptionText.getText());
		if(descriptionText.getText().isEmpty()) {
			descriptionText.setText(i18n.translate("project.machines.machineDescriptionView.placeholder"));
		}
	}

	public static DescriptionView getTraceDescriptionView(ReplayTrace trace, StageManager stageManager, TraceFileHandler traceFileHandler, I18n i18n, Runnable handleClose) {
		final DescriptionView descriptionView = new DescriptionView(stageManager, i18n);

		descriptionView.setName(trace.getName());
		descriptionView.setOnClose(handleClose);
		try {
			descriptionView.setDescription(trace.load().getDescription());
		} catch (IOException exc) {
			traceFileHandler.showLoadError(trace, exc);
		}

		descriptionView.descriptionProperty().addListener((o, from, to) -> {
			try {
				trace.saveModified(trace.load().changeDescription(to));
			} catch (IOException exc) {
				stageManager.makeExceptionAlert(exc, "traceSave.buttons.saveTrace.error", "traceSave.buttons.saveTrace.error.msg").show();
			}
		});

		return descriptionView;
	}
}
