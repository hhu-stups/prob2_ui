package de.prob2.ui.project.machines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.ViewCodeStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.sharedviews.DescriptionView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class MachinesTab extends Tab {
	private final class MachinesItem extends ListCell<Machine> {
		@FXML private Label nameLabel;
		@FXML private BindableGlyph statusIcon;
		@FXML private Label locationLabel;
		@FXML private ContextMenu contextMenu;
		@FXML private Menu startAnimationMenu;
		@FXML private MenuItem showInternalItem;
		
		private ObjectProperty<Machine> machineProperty;
		
		private MachinesItem() {
			this.machineProperty = new SimpleObjectProperty<>(this, "machine", null);
			stageManager.loadFXML(this, "machines_item.fxml");
		}
		
		@FXML
		private void initialize() {
			currentProject.currentMachineProperty().addListener(o -> this.refresh());
			this.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					boolean saved = injector.getInstance(BEditorView.class).savedProperty().get();
					if(!saved && !confirmSave()) {
						return;
					}
					startMachine(this.machineProperty.get());
				} else if(showMachineView && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 1) {
					showMachineView(this.machineProperty.get());
					this.updateSelected(true);
				}
			});
			currentProject.preferencesProperty().addListener((o, from, to) -> updatePreferences(to));
			this.updatePreferences(currentProject.getPreferences());
			currentProject.currentMachineProperty().addListener((observable, from, to) -> showInternalItem.setDisable(to == null || machineProperty.get() != to));
			machineProperty.addListener((observable, from, to) -> showInternalItem.setDisable(to == null || to != currentProject.getCurrentMachine()));
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
		}
		
		@FXML
		private void handleShowDescription() {
			showMachineView(this.machineProperty.get());
			machinesList.getSelectionModel().select(this.machineProperty.get());
		}
		
		@FXML
		private void handleEditConfiguration() {
			injector.getInstance(EditMachinesDialog.class).editAndShow(this.machineProperty.get()).ifPresent(result -> showMachineView(this.machineProperty.get()));
		}
		
		@FXML
		private void handleRemove() {
			stageManager.makeAlert(Alert.AlertType.CONFIRMATION, "",
					"project.machines.machinesTab.alerts.removeMachineConfirmation.content", this.machineProperty.get().getName())
					.showAndWait().ifPresent(buttonType -> {
						if (buttonType.equals(ButtonType.OK)) {
							currentProject.removeMachine(this.machineProperty.get());
						}
					});
		}
		
		@FXML
		private void handleEditFileExternal() {
			injector.getInstance(ExternalEditor.class).open(currentProject.getLocation().resolve(this.machineProperty.get().getLocation()));
		}
		
		@FXML
		private void handleShowInternal() {
			final ViewCodeStage stage = injector.getInstance(ViewCodeStage.class);
			stage.setTitle(currentProject.getCurrentMachine().getName());
			stage.setCode();
			stage.show();
		}
		
		private void refresh() {
			if (Objects.equals(this.machineProperty.get(), currentProject.getCurrentMachine())) {
				if (!statusIcon.getStyleClass().contains("running")) {
					statusIcon.getStyleClass().add("running");
				}
				statusIcon.setIcon(FontAwesome.Glyph.SPINNER);
			} else {
				statusIcon.getStyleClass().remove("running");
				statusIcon.setIcon(FontAwesome.Glyph.PLAY);
			}
		}
		
		private void updatePreferences(final List<Preference> prefs) {
			startAnimationMenu.getItems().clear();
			
			final MenuItem defItem = new MenuItem();
			defItem.textProperty().bind(Preference.DEFAULT.nameProperty());
			defItem.setOnAction(e -> {
				currentProject.startAnimation(this.machineProperty.get(), Preference.DEFAULT);
				this.machineProperty.get().setLastUsedPreferenceName(Preference.DEFAULT.getName());
			});
			startAnimationMenu.getItems().add(defItem);
			
			for (Preference preference : prefs) {
				final MenuItem menuItem = new MenuItem();
				menuItem.textProperty().bind(preference.nameProperty());
				// Disable mnemonic parsing so preferences with underscores in their names are displayed properly.
				menuItem.setMnemonicParsing(false);
				menuItem.setOnAction(e -> {
					currentProject.startAnimation(this.machineProperty.get(), preference);
					this.machineProperty.get().setLastUsedPreferenceName(preference.getName());
				});
				startAnimationMenu.getItems().add(menuItem);
			}
		}
		
		@Override
		protected void updateItem(final Machine item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.machineProperty.set(null);
				this.nameLabel.textProperty().unbind();
				this.nameLabel.setText(null);
				this.statusIcon.setVisible(false);
				this.locationLabel.setText(null);
				this.setContextMenu(null);
			} else {
				this.machineProperty.set(item);
				this.refresh();
				this.nameLabel.textProperty().bind(Bindings.format(
					"%s : %s",
					machineProperty.get().lastUsedPreferenceNameProperty(),
					machineProperty.get().nameProperty()
				));
				this.statusIcon.setVisible(true);
				this.locationLabel.setText(machineProperty.get().getLocation().toString());
				this.setContextMenu(contextMenu);
			}
		}

		private boolean confirmSave() {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
					"common.alerts.unsavedMachineChanges.header",
					"common.alerts.unsavedMachineChanges.content");
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MachinesTab.class);
	
	@FXML private ListView<Machine> machinesList;
	@FXML private SplitPane splitPane;
	@FXML private HelpButton helpButton;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;

	private boolean showMachineView;

	@Inject
	private MachinesTab(
		final CurrentTrace currentTrace,
		final CurrentProject currentProject,
		final StageManager stageManager,
		final FileChooserManager fileChooserManager,
		final Injector injector
	) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());

		splitPane.disableProperty().bind(injector.getInstance(MachineLoader.class).loadingProperty());

		machinesList.setCellFactory(lv -> this.new MachinesItem());
		machinesList.itemsProperty().bind(currentProject.machinesProperty());
		machinesList.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				startMachine(machinesList.getSelectionModel().getSelectedItem());
			}
		});
		injector.getInstance(DisablePropertyController.class).addDisableProperty(machinesList.disableProperty());
		currentProject.machinesProperty().addListener((observable, from, to) -> {
			Node node = splitPane.getItems().get(0);
			if (node instanceof DescriptionView && !to.contains((Machine) ((DescriptionView) node).getDescribable())) {
				closeMachineView();
			}
		});
	}

	@FXML
	private void createMachine() {
		final Path selected = fileChooserManager.showSaveMachineChooser(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}
		final String[] split = selected.getFileName().toString().split("\\.");
		final String machineName = split[0];
		final Set<String> machineNamesSet = currentProject.getMachines().stream()
			.map(Machine::getName)
			.collect(Collectors.toSet());
		int i = 1;
		String nameInProject = machineName;
		while (machineNamesSet.contains(nameInProject)) {
			nameInProject = String.format("%s (%d)", machineName, i);
			i++;
		}
		final Path relative = currentProject.getLocation().relativize(selected);
		final Machine machine;
		try {
			machine = new Machine(nameInProject, "", relative);
		} catch (IllegalArgumentException e) {
			LOGGER.info("User tried to create a machine with an invalid extension", e);
			final String extension = com.google.common.io.Files.getFileExtension(relative.getFileName().toString());
			stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.machines.machinesTab.alerts.invalidMachineExtension.content", extension).show();
			return;
		}
		
		try {
			Files.write(selected, Arrays.asList("MACHINE " + machineName, "END"));
		} catch (IOException e) {
			LOGGER.error("Could not create machine file", e);
			stageManager.makeExceptionAlert(e, "project.machines.machinesTab.alerts.couldNotCreateMachine.content");
			return;
		}
		currentProject.addMachine(machine);
	}

	@FXML
	void addMachine() {
		final Path selected = fileChooserManager.showOpenMachineChooser(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}

		final Path relative = currentProject.getLocation().relativize(selected);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			stageManager.makeAlert(Alert.AlertType.ERROR, "project.machines.machinesTab.alerts.machineAlreadyExists.header",
					"project.machines.machinesTab.alerts.machineAlreadyExists.content", relative)
					.showAndWait();
			return;
		}
		final Set<String> machineNamesSet = currentProject.getMachines().stream()
			.map(Machine::getName)
			.collect(Collectors.toSet());
		String[] n = relative.getFileName().toString().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = String.format("%s (%d)", n[0], i);
			i++;
		}
		currentProject.addMachine(new Machine(name, "", relative));
	}

	public void closeMachineView() {
		if (showMachineView) {
			splitPane.getItems().remove(0);
			machinesList.getSelectionModel().clearSelection();
			showMachineView = false;
		}
	}

	void showMachineView(final Machine machine) {
		if(showMachineView) {
			closeMachineView();
		}
		splitPane.getItems().add(0, new DescriptionView(machine, this::closeMachineView, stageManager, injector));
		showMachineView = true;
	}
	
	private void startMachine(final Machine machine) {
		if (machine != null) {
			currentProject.startAnimation(machine, currentProject.get().getPreference(machine.getLastUsedPreferenceName()));
		}
	}
}
