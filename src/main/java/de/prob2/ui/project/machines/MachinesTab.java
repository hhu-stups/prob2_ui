package de.prob2.ui.project.machines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import javafx.beans.binding.BooleanBinding;
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
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			statusIcon.visibleProperty().bind(machineProperty.isNotNull());
			this.contextMenuProperty().bind(Bindings.when(machineProperty.isNull()).then((ContextMenu)null).otherwise(contextMenu));
			
			final BooleanBinding machineIsCurrent = machineProperty.isEqualTo(currentProject.currentMachineProperty());
			showInternalItem.disableProperty().bind(machineIsCurrent.not().or(currentTrace.isNull()));
			statusIcon.iconProperty().bind(Bindings.when(machineIsCurrent).then(FontAwesome.Glyph.SPINNER).otherwise(FontAwesome.Glyph.PLAY));
			machineIsCurrent.addListener((o, from, to) -> {
				if (to) {
					statusIcon.getStyleClass().add("running");
				} else {
					statusIcon.getStyleClass().remove("running");
				}
			});
		}
		
		@FXML
		private void handleShowDescription() {
			showMachineView(this.machineProperty.get());
			machinesList.getSelectionModel().select(this.machineProperty.get());
		}
		
		@FXML
		private void handleEditConfiguration() {
			final EditMachinesDialog editDialog = injector.getInstance(EditMachinesDialog.class);
			editDialog.initOwner(MachinesTab.this.getTabPane().getScene().getWindow());
			editDialog.editAndShow(this.machineProperty.get()).ifPresent(result -> showMachineView(this.machineProperty.get()));
		}
		
		@FXML
		private void handleRemove() {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, "",
					"project.machines.machinesTab.alerts.removeMachineConfirmation.content", this.machineProperty.get().getName());
			alert.initOwner(MachinesTab.this.getContent().getScene().getWindow());
			alert.showAndWait().ifPresent(buttonType -> {
				if (buttonType.equals(ButtonType.OK)) {
					currentProject.removeMachine(this.machineProperty.get());
				}
			});
		}
		
		@FXML
		private void handleEditFileExternal() {
			injector.getInstance(ExternalEditor.class).open(currentProject.get().getAbsoluteMachinePath(this.machineProperty.get()));
		}
		
		@FXML
		private void handleShowInternal() {
			final ViewCodeStage stage = injector.getInstance(ViewCodeStage.class);
			stage.setTitle(currentProject.getCurrentMachine().getName());
			stage.setCode();
			stage.show();
		}
		
		private void updatePreferences(final List<Preference> prefs) {
			startAnimationMenu.getItems().setAll(Stream.concat(Stream.of(Preference.DEFAULT), prefs.stream())
				.map(preference -> {
					final MenuItem menuItem = new MenuItem();
					menuItem.textProperty().bind(preference.nameProperty());
					// Disable mnemonic parsing so preferences with underscores in their names are displayed properly.
					menuItem.setMnemonicParsing(false);
					menuItem.setOnAction(e -> {
						currentProject.startAnimation(this.machineProperty.get(), preference);
						this.machineProperty.get().setLastUsedPreferenceName(preference.getName());
					});
					return menuItem;
				})
				.collect(Collectors.toList()));
		}
		
		@Override
		protected void updateItem(final Machine item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.machineProperty.set(null);
				this.nameLabel.textProperty().unbind();
				this.nameLabel.setText(null);
				this.locationLabel.setText(null);
			} else {
				this.machineProperty.set(item);
				this.nameLabel.textProperty().bind(
					Bindings.when(machineProperty.get().lastUsedPreferenceNameProperty().isEqualTo("default"))
					.then(machineProperty.get().nameProperty())
					.otherwise(Bindings.format("%s (%s)", machineProperty.get().nameProperty(), machineProperty.get().lastUsedPreferenceNameProperty()))
				);
				this.locationLabel.setText(machineProperty.get().getLocation().toString());
			}
		}

		private boolean confirmSave() {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
					"common.alerts.unsavedMachineChanges.header",
					"common.alerts.unsavedMachineChanges.content");
			alert.initOwner(MachinesTab.this.getContent().getScene().getWindow());
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
		helpButton.setHelpContent("project", "Machines");

		splitPane.disableProperty().bind(injector.getInstance(MachineLoader.class).loadingProperty());

		machinesList.setCellFactory(lv -> this.new MachinesItem());
		machinesList.itemsProperty().bind(currentProject.machinesProperty());
		machinesList.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				startMachine(machinesList.getSelectionModel().getSelectedItem());
			}
		});
		machinesList.disableProperty().bind(injector.getInstance(DisablePropertyController.class).disableProperty());
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
			final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.machines.machinesTab.alerts.invalidMachineExtension.content", extension);
			alert.initOwner(this.getContent().getScene().getWindow());
			alert.show();
			return;
		}
		
		try {
			Files.write(selected, Arrays.asList("MACHINE " + machineName, "END"));
		} catch (IOException e) {
			LOGGER.error("Could not create machine file", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "project.machines.machinesTab.alerts.couldNotCreateMachine.content");
			alert.initOwner(this.getContent().getScene().getWindow());
			alert.show();
			return;
		}
		currentProject.addMachine(machine);
		currentProject.startAnimation(machine, Preference.DEFAULT);
	}

	@FXML
	void addMachine() {
		final Path selected = fileChooserManager.showOpenMachineChooser(this.getContent().getScene().getWindow());
		if (selected == null) {
			return;
		}

		final Path relative = currentProject.getLocation().relativize(selected);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR, "project.machines.machinesTab.alerts.machineAlreadyExists.header",
					"project.machines.machinesTab.alerts.machineAlreadyExists.content", relative);
			alert.initOwner(this.getContent().getScene().getWindow());
			alert.showAndWait();
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
		final Machine machine = new Machine(name, "", relative);
		currentProject.addMachine(machine);
		currentProject.startAnimation(machine, Preference.DEFAULT);
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
