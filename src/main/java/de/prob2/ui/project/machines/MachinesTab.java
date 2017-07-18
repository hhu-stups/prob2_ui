package de.prob2.ui.project.machines;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.EditMenu;
import de.prob2.ui.menu.FileAsker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

@Singleton
public class MachinesTab extends Tab {
	@FXML
	private VBox machinesVBox;
	@FXML
	private StackPane noMachinesStack;
	@FXML
	private SplitPane splitPane;
	@FXML
	private Button addMachineButton;
	@FXML
	private HelpButton helpButton;

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Injector injector;

	@Inject
	private MachinesTab(final StageManager stageManager, final CurrentProject currentProject, final Injector injector) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() throws URISyntaxException {
		helpButton.setPathToHelp(ProB2.class.getClassLoader().getResource("help/HelpMain.html").toURI().toString());
		noMachinesStack.managedProperty().bind(currentProject.machinesProperty().emptyProperty());
		noMachinesStack.visibleProperty().bind(currentProject.machinesProperty().emptyProperty());

		currentProject.machinesProperty().addListener((observable, from, to) -> {
			Node node = splitPane.getItems().get(0);
			if (node instanceof MachineView && !to.contains(((MachineView) node).getMachine())) {
				closeMachineView();
			}
			
			machinesVBox.getChildren().clear();
			for (Machine machine : to) {
				MachinesItem machinesItem = new MachinesItem(machine, stageManager);
				machinesVBox.getChildren().add(machinesItem);

				final MenuItem editMachineMenuItem = new MenuItem("Edit Machine Configuration");
				editMachineMenuItem.setOnAction(event -> injector.getInstance(EditMachinesDialog.class)
						.editAndShow(machine).ifPresent(result -> {
					machinesItem.refresh();
					showMachineView(machine);
				}));

				final MenuItem removeMachineMenuItem = new MenuItem("Remove Machine");
				removeMachineMenuItem.setOnAction(event -> currentProject.removeMachine(machine));

				final MenuItem editFileMenuItem = new MenuItem("Edit Machine File");
				editFileMenuItem.setOnAction(event -> injector.getInstance(EditMenu.class).showEditorStage(machine));

				final MenuItem editExternalMenuItem = new MenuItem("Edit Machine File in External Editor");
				editExternalMenuItem
						.setOnAction(event -> injector.getInstance(EditMenu.class).showExternalEditor(machine));

				final Menu startAnimationMenu = new Menu("Start Animation...");

				ContextMenu contextMenu = new ContextMenu(editMachineMenuItem, removeMachineMenuItem, editFileMenuItem,
						editExternalMenuItem, startAnimationMenu);

				machinesItem.setOnMouseClicked(event -> {
					if (event.getButton().equals(MouseButton.SECONDARY) && event.getClickCount() == 1) {
						updateAnimationMenu(startAnimationMenu, machine);
						contextMenu.show(machinesItem, event.getScreenX(), event.getScreenY());
					} else if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
						showMachineView(machine);
					}
				});
			}
		});

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (addMachineButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
	}

	private void updateAnimationMenu(final Menu startAnimationMenu, Machine machine) {
		startAnimationMenu.getItems().clear();
		for (Runconfiguration runconfiguration : currentProject.getRunconfigurations(machine)) {
			final MenuItem item = new MenuItem(runconfiguration.toString());
			item.setOnAction(e -> currentProject.startAnimation(runconfiguration));
			startAnimationMenu.getItems().add(item);
		}
		if (startAnimationMenu.getItems().size() == 0) {
			startAnimationMenu.setDisable(true);
		}
	}

	@FXML
	void addMachine() {
		final File selected = FileAsker.askForMachine(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}

		final Path projectLocation = currentProject.getLocation().toPath();
		final Path absolute = selected.toPath();
		final Path relative = projectLocation.relativize(absolute);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			stageManager.makeAlert(Alert.AlertType.ERROR,
					"The machine \"" + relative + "\" already exists in the current project.").showAndWait();
			return;
		}
		injector.getInstance(AddMachinesDialog.class).showAndWait(relative).ifPresent(currentProject::addMachine);
	}

	private void showMachineView(Machine machine) {
		if (splitPane.getItems().size() >= 2) {
			splitPane.getItems().remove(0);
		}
		splitPane.getItems().add(0, new MachineView(machine, stageManager, injector));
	}

	void closeMachineView() {
		if (splitPane.getItems().get(0) instanceof MachineView) {
			splitPane.getItems().remove(0);
		}
	}
}
