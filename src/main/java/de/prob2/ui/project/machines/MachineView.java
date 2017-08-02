package de.prob2.ui.project.machines;

import com.google.inject.Injector;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class MachineView extends AnchorPane {
	
	@FXML
	private Label titelLabel;
	@FXML
	private Text descriptionText;
	@FXML
	private Button closeMachineViewButton;
	
	private final MachinesItem machinesItem;
	private final Machine machine;
	private final Injector injector;
	
	MachineView(final MachinesItem machinesItem, final StageManager stageManager, final Injector injector) {
		this.machinesItem = machinesItem;
		this.machine = machinesItem.getMachine();
		this.injector = injector;
		stageManager.loadFXML(this, "machine_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(machine.getName());
		descriptionText.setText(machine.getDescription());
		
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (closeMachineViewButton.getGraphic())).glyphSizeProperty().bind(fontsize);
	}

	@FXML
	public void closeMachineView() {
		injector.getInstance(MachinesTab.class).closeMachineView();
	}

	Machine getMachine() {
		return this.machine;
	}
	
	MachinesItem getMachinesItem() {
		return machinesItem;
	}
}
