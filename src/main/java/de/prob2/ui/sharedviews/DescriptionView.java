package de.prob2.ui.sharedviews;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.machines.MachinesTab;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class DescriptionView extends AnchorPane {

	public interface Describable {
		String getName();
		String getDescription();
	}

	@FXML
	private Label titelLabel;
	@FXML
	private Text descriptionText;

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
		descriptionText.setText(describable.getDescription().isEmpty()? bundle.getString("project.machines.machineDescriptionView.placeholder") : describable.getDescription());
	}

	@FXML
	public void closeDescriptionView() {
		closeMethod.run();
	}

	public Describable getDescribable() {
		return this.describable;
	}
}
