package de.prob2.ui.project.machines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.menu.ViewCodeStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.preferences.Preference;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
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
		@FXML private MenuItem startAnimationMenu;
		@FXML private Menu startAnimationWithPreferencesMenu;
		@FXML private MenuItem showInternalItem;

		private ObjectProperty<Machine> machineProperty;

		private MachinesItem() {
			this.machineProperty = new SimpleObjectProperty<>(this, "machine", null);
			stageManager.loadFXML(this, "machines_item.fxml");
		}

		@FXML
		private void initialize() {
			this.setOnMouseClicked(event -> {
				Machine machine = this.machineProperty.get();
				if (machine != null && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					currentProject.reloadMachine(machine);
				}
			});
			currentProject.preferencesProperty().addListener((o, from, to) -> updatePreferences(to));
			this.startAnimationMenu.setOnAction(e -> currentProject.reloadMachine(this.machineProperty.get()));
			this.startAnimationWithPreferencesMenu.disableProperty().bind(currentProject.preferencesProperty().emptyProperty());
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
		private void handleEditConfiguration() {
			final EditMachinesDialog editDialog = injector.getInstance(EditMachinesDialog.class);
			editDialog.initOwner(MachinesTab.this.getTabPane().getScene().getWindow());
			editDialog.editAndShow(this.machineProperty.get());
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
		private void handleRevealFileInExplorer() {
			injector.getInstance(RevealInExplorer.class).revealInExplorer(currentProject.get().getAbsoluteMachinePath(this.machineProperty.get()));
		}

		@FXML
		private void handleShowInternal() {
			injector.getInstance(ViewCodeStage.class).show();
		}

		private void updatePreferences(final List<Preference> prefs) {
			startAnimationWithPreferencesMenu.getItems().setAll(Stream.concat(Stream.of(Preference.DEFAULT), prefs.stream())
				.map(preference -> {
					final MenuItem menuItem = new MenuItem();
					menuItem.textProperty().bind(preference.nameProperty());
					// Disable mnemonic parsing so preferences with underscores in their names are displayed properly.
					menuItem.setMnemonicParsing(false);
					menuItem.setOnAction(e -> {
						MachinesTab.this.currentProject.reloadMachine(this.machineProperty.get(), preference);
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
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(MachinesTab.class);

	@FXML private ListView<Machine> machinesList;
	@FXML private Button moveUpBtn;
	@FXML private Button moveDownBtn;
	@FXML private HelpButton helpButton;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;

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

		machinesList.disableProperty().bind(injector.getInstance(MachineLoader.class).loadingProperty());
		machinesList.setCellFactory(lv -> this.new MachinesItem());
		machinesList.itemsProperty().bind(currentProject.machinesProperty());
		machinesList.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				final Machine machine = machinesList.getSelectionModel().getSelectedItem();
				if (machine != null) {
					currentProject.reloadMachine(machine);
				}
			}
		});
		machinesList.getSelectionModel().selectedIndexProperty().addListener((observable, from, to) -> {
			if (!(to instanceof Integer)) {
				moveUpBtn.setDisable(true);
				moveDownBtn.setDisable(true);
				return;
			}

			int n = (int) to;
			int size = machinesList.getItems().size();
			if (n < 0 || n >= size) {
				moveUpBtn.setDisable(true);
				moveDownBtn.setDisable(true);
				return;
			}

			moveUpBtn.setDisable(n == 0);
			moveDownBtn.setDisable(n == (size - 1));
		});
	}

	@FXML
	private void createMachine() {
		if (!currentProject.confirmMachineReplace()) {
			return;
		}

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
			nameInProject = String.format(Locale.ROOT, "%s (%d)", machineName, i);
			i++;
		}
		final Path relative = currentProject.getLocation().relativize(selected);
		final Machine machine;
		try {
			machine = new Machine(nameInProject, "", relative);
		} catch (IllegalArgumentException e) {
			LOGGER.info("User tried to create a machine with an invalid extension", e);
			final String extension = MoreFiles.getFileExtension(relative);
			final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.machines.machinesTab.alerts.invalidMachineExtension.content", extension);
			alert.initOwner(this.getContent().getScene().getWindow());
			alert.show();
			return;
		}

		try {
			final String extension = MoreFiles.getFileExtension(relative);
			List<String> content = Collections.emptyList();
			// FIXME Nonworking formalisms/file extensions have been removed as unsupported from FileChooserManager. Issues with formalisms are left here intentionally if support will be added in future
			switch (extension) {
				// Classical B
				case "mch":
					content = Arrays.asList("MACHINE " + machineName, "END");
					break;
				/*case "ref":
					// TODO Throws "expecting identifier" error since no machine has been named -> useful?
					content = Arrays.asList("REFINEMENT " + machineName, "REFINES", "END");
					break;
				case "imp":
					// TODO Throws "expecting identifier" error since no machine has been named -> useful?
					content = Arrays.asList("IMPLEMENTATION " + machineName, "REFINES", "END");
					break;
				case "sys":
					// TODO Treated as Event-B -> error? (ALLOW_NEW_OPERATIONS_IN_REFINEMENT to TRUE)
					content = Arrays.asList("SYSTEM", "    " + machineName, "END");
					break;
				case "def":
					// TODO Throws "Expecting identifier literal, double quotation" error (expects to include definitions?)
					content = Collections.singletonList("DEFINITIONS");
					break;*/

				/*// Event-B
				// TODO Creating Event-B files not working without errors (Is it useful to invest in translating?)
				case "bum":
					// (Missing bcm error atm)
				case "buc":
					// (Missing bcc error atm)
				case "eventb":
					// (No proper load event atm)
					break;*/

				// CSP
				case "csp":
				case "cspm":
					break;

				// TLA
				case "tla":
					content = Arrays.asList("---- MODULE " + machineName + " ----", "====");
					break;

				// B Rules
				case "rmch":
					content = Arrays.asList("RULES_MACHINE " + machineName, "END//RULES_MACHINE");
					break;

				// XTL Prolog
				case "P":
				case "pl":
					content = Collections.singletonList("start(0).");
					break;

				// Z
				case "zed":
				case "tex":
					content = Arrays.asList(
						"\\documentclass{article}",
						"\\usepackage{fuzz}",
						"\\begin{document}",
						"This defines a deferred set ID:",
						"\\begin{zed}", "  [ID]", "\\end{zed}",
						"This schema represents the state of the model:",
						"\\begin{schema}{State}", "x: \\power ID \\\\", "\\end{schema}",
						"This schema is the initialisation:",
						"\\begin{schema}{Init}", "  State", "   \\where", "   x =\\emptyset", "\\end{schema}",
						"This schema is an operation with a parameter:",
						"\\begin{schema}{Enter}", "  \\Delta State\\\\", "  new? : ID",
						"   \\where", "   new? \\notin x\\\\", "   x' = x \\cup \\{new?\\}",
						"\\end{schema}",
						"\\end{document}"
					);
					break;

				/*// Fuzz
				case "fuzz":
					// TODO Fix unexpected expression error (in mode top given)
					// content = Arrays.asList("(GIVEN 0", "  )", "(SDEF 0", "  (SHEAD 0", "    )", "  (BODY 0", "    ))");
					// ("No Schema" error atm)
					break;*/

				// Alloy
				case "als":
					break;

				// Unknown
				default:
					break;
			}
			Files.write(selected, content);
		} catch (IOException e) {
			LOGGER.error("Could not create machine file", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "project.machines.machinesTab.alerts.couldNotCreateMachine.content");
			alert.initOwner(this.getContent().getScene().getWindow());
			alert.show();
			return;
		}
		currentProject.addMachine(machine);
		// call startAnimation directly because we already asked the user for confirmation
		currentProject.startAnimation(machine);
	}

	@FXML
	void addMachine() {
		if (!currentProject.confirmMachineReplace()) {
			return;
		}

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
			name = String.format(Locale.ROOT, "%s (%d)", n[0], i);
			i++;
		}
		final Machine machine = new Machine(name, "", relative);
		currentProject.addMachine(machine);
		// call startAnimation directly because we already asked the user for confirmation
		currentProject.startAnimation(machine);
	}

	@FXML
	void moveUp() {
		int n = machinesList.getSelectionModel().getSelectedIndex();
		int size = machinesList.getItems().size();
		if (n <= 0 || n >= size) {
			return;
		}

		swapMachineOrder(n, n - 1);
		machinesList.getSelectionModel().select(n - 1);
	}

	@FXML
	void moveDown() {
		int n = machinesList.getSelectionModel().getSelectedIndex();
		int size = machinesList.getItems().size();
		if (n < 0 || n >= (size - 1)) {
			return;
		}

		swapMachineOrder(n, n + 1);
		machinesList.getSelectionModel().select(n + 1);
	}

	private void swapMachineOrder(int i, int j) {
		if (i != j) {
			List<Machine> machines = new ArrayList<>(currentProject.getMachines());
			Collections.swap(machines, i, j);
			currentProject.changeMachineOrder(machines);
		}
	}
}
