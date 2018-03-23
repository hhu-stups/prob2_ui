package de.prob2.ui.project.machines;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.EditPreferencesProvider;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.statusbar.StatusBar.LoadingStatus;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

@Singleton
public class MachinesTab extends Tab {
	private final class MachinesItem extends ListCell<Machine> {
		@FXML private Label nameLabel;
		@FXML private FontAwesomeIconView runningIcon;
		@FXML private Label locationLabel;
		@FXML private ContextMenu contextMenu;
		@FXML private Menu startAnimationMenu;
		
		private Machine machine;
		
		private MachinesItem() {
			stageManager.loadFXML(this, "machines_item.fxml");
		}
		
		@FXML
		private void initialize() {
			currentProject.currentMachineProperty().addListener(o -> this.refresh());
			this.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					startMachine(this.machine);
				}
			});
			currentProject.preferencesProperty().addListener((o, from, to) -> updatePreferences(to));
			this.updatePreferences(currentProject.getPreferences());
		}
		
		@FXML
		private void handleShowDescription() {
			showMachineView(this.machine);
			machinesList.getSelectionModel().select(this.machine);
		}
		
		@FXML
		private void handleEditConfiguration() {
			injector.getInstance(EditMachinesDialog.class).editAndShow(this.machine).ifPresent(result -> showMachineView(this.machine));
		}
		
		@FXML
		private void handleRemove() {
			stageManager.makeAlert(Alert.AlertType.CONFIRMATION, String.format(bundle.getString("project.machines.removeDialog.message"), this.machine.getName())).showAndWait().ifPresent(buttonType -> {
				if (buttonType.equals(ButtonType.OK)) {
					currentProject.removeMachine(this.machine);
				}
			});
		}
		
		@FXML
		private void handleEditFileExternal() {
			injector.getInstance(EditPreferencesProvider.class).showExternalEditor(currentProject.getLocation().resolve(this.machine.getPath()));
		}
		
		private void refresh() {
			if (Objects.equals(this.machine, currentProject.getCurrentMachine())) {
				if (!runningIcon.getStyleClass().contains("running")) {
					runningIcon.getStyleClass().add("running");
				}
			} else {
				runningIcon.getStyleClass().remove("running");
			}
		}
		
		private void updatePreferences(final List<Preference> prefs) {
			startAnimationMenu.getItems().clear();
			
			final MenuItem defItem = new MenuItem(Preference.DEFAULT.getName());
			defItem.setOnAction(e -> {
				currentProject.startAnimation(this.machine, Preference.DEFAULT);
				this.machine.setLastUsed(Preference.DEFAULT);
			});
			startAnimationMenu.getItems().add(defItem);
			
			for (Preference preference : prefs) {
				final MenuItem menuItem = new MenuItem(preference.getName());
				// Disable mnemonic parsing so preferences with underscores in their names are displayed properly.
				menuItem.setMnemonicParsing(false);
				menuItem.setOnAction(e -> {
					currentProject.startAnimation(this.machine, preference);
					this.machine.setLastUsed(preference);
				});
				startAnimationMenu.getItems().add(menuItem);
			}
		}
		
		@Override
		protected void updateItem(final Machine item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.machine = null;
				this.nameLabel.textProperty().unbind();
				this.nameLabel.setText(null);
				this.runningIcon.setVisible(false);
				this.locationLabel.setText(null);
				this.setContextMenu(null);
			} else {
				this.machine = item;
				this.refresh();
				this.nameLabel.textProperty().bind(Bindings.format(
					"%s : %s",
					Bindings.createStringBinding(() -> machine.getLastUsed().getName(), machine.lastUsedProperty()),
					machine.nameProperty()
				));
				this.runningIcon.setVisible(true);
				this.locationLabel.setText(machine.getPath().toString());
				this.setContextMenu(contextMenu);
			}
		}

		private void showMachineView(final Machine machine) {
			closeMachineView();
			splitPane.getItems().add(0, new MachineView(machine, stageManager, injector));
		}
	}
	
	@FXML private ListView<Machine> machinesList;
	@FXML private SplitPane splitPane;
	@FXML private HelpButton helpButton;

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Injector injector;

	@Inject
	private MachinesTab(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentProject currentProject, final Injector injector) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());

		injector.getInstance(StatusBar.class).loadingStatusProperty()
				.addListener((observable, from, to) -> splitPane.setDisable(to == LoadingStatus.LOADING_FILE));

		machinesList.setCellFactory(lv -> this.new MachinesItem());
		machinesList.itemsProperty().bind(currentProject.machinesProperty());
		machinesList.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				startMachine(machinesList.getSelectionModel().getSelectedItem());
			}
		});
		currentProject.machinesProperty().addListener((observable, from, to) -> {
			Node node = splitPane.getItems().get(0);
			if (node instanceof MachineView && !to.contains(((MachineView) node).getMachine())) {
				closeMachineView();
			}
		});
	}

	@FXML
	void addMachine() {
		final File selected = stageManager.showOpenMachineChooser(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}

		final Path projectLocation = currentProject.getLocation();
		final Path absolute = selected.toPath();
		final Path relative = projectLocation.relativize(absolute);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			stageManager
					.makeAlert(Alert.AlertType.ERROR,
							String.format(bundle.getString("project.machines.error.machineAlreadyExists"), relative))
					.showAndWait();
			return;
		}
		final Set<String> machineNamesSet = currentProject.getMachines().stream().map(Machine::getName)
				.collect(Collectors.toSet());
		String[] n = relative.toFile().getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = String.format(bundle.getString("project.machines.nameSuffix"), n[0], i);
			i++;
		}
		currentProject.addMachine(new Machine(name, "", relative));
	}

	void closeMachineView() {
		if (splitPane.getItems().get(0) instanceof MachineView) {
			splitPane.getItems().remove(0);
			machinesList.getSelectionModel().clearSelection();
		}
	}
	
	private void startMachine(final Machine machine) {
		if (machine != null) {
			currentProject.startAnimation(machine, machine.getLastUsed());
		}
	}
}
