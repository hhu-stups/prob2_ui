package de.prob2.ui.project.machines;

import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.EditMenu;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;
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
	private final ResourceBundle bundle;
	private final Injector injector;

	@Inject
	private MachinesTab(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final Injector injector) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("Project.md.html");
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

				final MenuItem editMachineMenuItem = new MenuItem(bundle.getString("project.machines.tab.menu.editMachineConfiguration"));
				editMachineMenuItem.setOnAction(event -> injector.getInstance(EditMachinesDialog.class)
						.editAndShow(machine).ifPresent(result -> {
					machinesItem.refresh();
					showMachineView(machinesItem);
				}));

				final MenuItem removeMachineMenuItem = new MenuItem(bundle.getString("project.machines.tab.menu.removeMachine"));
				removeMachineMenuItem.setOnAction(event -> currentProject.removeMachine(machine));

				final MenuItem editFileMenuItem = new MenuItem(bundle.getString("project.machines.tab.menu.editMachineFile"));
				editFileMenuItem.setOnAction(event -> injector.getInstance(EditMenu.class).showEditorStage(machine));

				final MenuItem editExternalMenuItem = new MenuItem(bundle.getString("project.machines.tab.menu.editMachineFileInExternalEditor"));
				editExternalMenuItem
						.setOnAction(event -> injector.getInstance(EditMenu.class).showExternalEditor(machine));

				final Menu startAnimationMenu = new Menu(bundle.getString("project.machines.tab.menu.startAnimation"));

				final MenuItem showDescription = new MenuItem(bundle.getString("project.machines.tab.menu.machineDescription"));
				showDescription.setOnAction(event -> {
					showMachineView(machinesItem);
					machinesItem.setId("machines-item-selected");
				});

				ContextMenu contextMenu = new ContextMenu(showDescription, editMachineMenuItem, removeMachineMenuItem, editFileMenuItem,
						editExternalMenuItem, startAnimationMenu);

				machinesItem.setOnMouseClicked(event -> {
					if (event.getButton().equals(MouseButton.SECONDARY) && event.getClickCount() == 1) {
						updateAnimationMenu(startAnimationMenu, machine);
						contextMenu.show(machinesItem, event.getScreenX(), event.getScreenY());
					} else if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
						currentProject.startAnimation(new Runconfiguration(machine, new DefaultPreference()));
					}
				});
			}
		});

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (addMachineButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
	}

	private void updateAnimationMenu(final Menu startAnimationMenu, Machine machine) {
		startAnimationMenu.getItems().clear();

		DefaultPreference defaultPreference = new DefaultPreference();
		final Runconfiguration defRunconfiguration = new Runconfiguration(machine, defaultPreference);
		final MenuItem defItem = new MenuItem(defaultPreference.toString());
		defItem.setOnAction(e -> currentProject.startAnimation(defRunconfiguration));
		startAnimationMenu.getItems().add(defItem);

		if(currentProject.getPreferences().isEmpty())
			return;
		for(Preference preference : currentProject.getPreferences()){
			final MenuItem item = new MenuItem(preference.toString());
			final Runconfiguration runconfiguration = new Runconfiguration(machine, preference);
			item.setOnAction(e -> currentProject.startAnimation(runconfiguration));
			startAnimationMenu.getItems().add(item);
		}
		if (startAnimationMenu.getItems().isEmpty()) {
			startAnimationMenu.setDisable(true);
		}
	}

	@FXML
	void addMachine() {
		final File selected = stageManager.showOpenMachineChooser(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}

		final Path projectLocation = currentProject.getLocation().toPath();
		final Path absolute = selected.toPath();
		final Path relative = projectLocation.relativize(absolute);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("project.machines.error.machineAlreadyExists"), relative)).showAndWait();
			return;
		}

		final Set<String> machineNamesSet = currentProject.getMachines().stream().map(Machine::getName).collect(Collectors.toSet());
		String[] n = relative.toFile().getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = String.format(bundle.getString("project.machines.nameSuffix"), n[0], i);
			i++;
		}
		currentProject.addMachine(new Machine(name, "", relative));
	}

	private void showMachineView(MachinesItem machinesItem) {
		closeMachineView();
		splitPane.getItems().add(0, new MachineView(machinesItem, stageManager, injector));
	}

	void closeMachineView() {
		if (splitPane.getItems().get(0) instanceof MachineView) {
			MachineView machineView = (MachineView) splitPane.getItems().get(0);		
			splitPane.getItems().remove(0);
			machineView.getMachinesItem().setId("machines-item");
		}
	}
}
