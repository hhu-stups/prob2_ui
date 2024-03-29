package de.prob2.ui.verifications.temporal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;
import de.prob.json.JsonMetadata;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.ltl.LTLData;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternParser;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternStage;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class TemporalView extends CheckingViewBase<TemporalFormulaItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("verifications.temporal.temporalView.contextMenu.check"));
			
			MenuItem showCounterExampleItem = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showCounterExample"));
			showCounterExampleItem.setOnAction(e -> currentTrace.set(itemsTable.getSelectionModel().getSelectedItem().getCounterExample()));
			showCounterExampleItem.setDisable(true);
			contextMenu.getItems().add(showCounterExampleItem);
			
			MenuItem showMessage = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResultItem().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);
			
			this.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showCounterExampleItem.disableProperty().bind(to.counterExampleProperty().isNull());
				}
			});
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TemporalView.class);
	
	private static final String LTL_FILE_EXTENSION = "prob2ltl";
	private static final String OLD_LTL_FILE_EXTENSION = "ltl";
	
	@FXML 
	private MenuButton addMenuButton;
	@FXML
	private MenuItem addFormulaButton;
	@FXML
	private MenuItem addPatternButton;
	@FXML
	private Button saveLTLButton;
	@FXML
	private Button loadLTLButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	@FXML
	private TableColumn<TemporalFormulaItem, String> formulaDescriptionColumn;
	@FXML
	private TableColumn<LTLPatternItem, Checked> patternStatusColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;

	private final StageManager stageManager;
	private final I18n i18n;
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final VersionInfo versionInfo;
	private final CurrentProject currentProject;
	private final FileChooserManager fileChooserManager;
	private final JacksonManager<LTLData> jacksonManager;
				
	@Inject
	private TemporalView(final StageManager stageManager, final I18n i18n, final Injector injector,
						 final CurrentTrace currentTrace, final VersionInfo versionInfo, final CurrentProject currentProject,
						 final DisablePropertyController disablePropertyController,
						 final CliTaskExecutor cliExecutor,
						 final FileChooserManager fileChooserManager,
						 final ObjectMapper objectMapper,
						 final JacksonManager<LTLData> jacksonManager) {
		super(i18n, disablePropertyController, currentTrace, currentProject, cliExecutor);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.versionInfo = versionInfo;
		this.currentProject = currentProject;
		this.fileChooserManager = fileChooserManager;
		this.jacksonManager = jacksonManager;
		jacksonManager.initContext(new JacksonManager.Context<>(objectMapper, LTLData.class, LTLData.FILE_TYPE, LTLData.CURRENT_FORMAT_VERSION) {
			@Override
			public boolean shouldAcceptOldMetadata() {
				return true;
			}

			@Override
			public ObjectNode convertOldData(final ObjectNode oldObject, final int oldVersion) {
				if (oldVersion <= 0) {
					for (final String fieldName : new String[] { "formulas", "patterns" }) {
						if (!oldObject.has(fieldName)) {
							throw new JsonConversionException("Not a valid LTL file - missing required field " + fieldName);
						}
					}
				}
				return oldObject;
			}
		});
		stageManager.loadFXML(this, "temporal_view.fxml");
	}

	@Override
	protected ObservableList<TemporalFormulaItem> getItemsProperty(Machine machine) {
		return machine.getMachineProperties().getTemporalFormulas();
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("verification", "LTL");
		setContextMenus();
		setBindings();
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			tvPattern.itemsProperty().unbind();
			if (from != null) {
				from.getMachineProperties().clearPatternManager();
			}
			if (to != null) {
				tvPattern.itemsProperty().bind(to.getMachineProperties().getLTLPatterns());
				managePatternTable(to.getMachineProperties().getLTLPatterns());
			} else {
				tvPattern.setItems(FXCollections.emptyObservableList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}
	
	/**
	 * Sets the context menus for the items LTLFormula and LTLPatterns
	 */
	private void setContextMenus() {
		itemsTable.setRowFactory(table -> new Row());
		
		tvPattern.setRowFactory(table -> {
			final TableRow<LTLPatternItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.remove"));
			removeItem.setOnAction(e -> {
				Machine machine = currentProject.getCurrentMachine();
				LTLPatternItem item = row.getItem();
				machine.getMachineProperties().getLTLPatterns().remove(item);
				LTLPatternParser.removePattern(item, machine);
				managePatternTable(machine.getMachineProperties().getLTLPatterns());
			});

			MenuItem openEditor = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.edit"));
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));
			
			MenuItem showMessage = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showParsingMessage"));
			showMessage.setOnAction(e -> row.getItem().getResultItem().showAlert(stageManager, i18n));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
				}
			});
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(openEditor, showMessage, removeItem)));
			return row;
		});
	}

	private void managePatternTable(ObservableList<LTLPatternItem> ltlPatternItems){
		if (ltlPatternItems.isEmpty()) {
			tvPattern.setVisible(false);
			tvPattern.setManaged(false);
		} else {
			tvPattern.setVisible(true);
			tvPattern.setManaged(true);
		}
	}

	private void setBindings() {
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		patternStatusColumn.setCellFactory(col -> new CheckedCell<>());
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		patternColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		addMenuButton.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));
		saveLTLButton.disableProperty().bind(emptyProperty.or(currentTrace.isNull().or(selectAll.selectedProperty().not())));
		loadLTLButton.disableProperty().bind(currentTrace.isNull());

		itemsTable.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));
	}
	
	@Override
	protected String configurationForItem(final TemporalFormulaItem item) {
		return item.getCode();
	}
	
	@Override
	protected void executeItemSync(final TemporalFormulaItem item, final ExecutionContext context) {
		item.execute(context);
		if (item.getCounterExample() != null) {
			currentTrace.set(item.getCounterExample());
		}
	}
	
	@FXML
	public void addPattern() {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		patternStage.showAndWait();
		final LTLPatternItem newItem = patternStage.getResult();
		if (newItem == null) {
			// User cancelled/closed the window
			return;
		}
		final Machine machine = currentProject.getCurrentMachine();
		if (machine.getMachineProperties().getLTLPatterns().stream().noneMatch(newItem::settingsEqual)) {
			LTLPatternParser.addPattern(newItem, machine);
			machine.getMachineProperties().getLTLPatterns().add(newItem);
			managePatternTable(machine.getMachineProperties().getLTLPatterns());
		} else {
			stageManager.makeAlert(Alert.AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content.pattern").show();
		}
	}
	
	@Override
	protected Optional<TemporalFormulaItem> showItemDialog(final TemporalFormulaItem oldItem) {
		TemporalFormulaStage formulaStage = injector.getInstance(TemporalFormulaStage.class);
		if (oldItem != null) {
			formulaStage.setData(oldItem);
		}
		formulaStage.showAndWait();
		return Optional.ofNullable(formulaStage.getResult());
	}
	
	private void showCurrentItemDialog(LTLPatternItem oldItem) {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		patternStage.setData(oldItem);
		patternStage.showAndWait();
		final LTLPatternItem changedItem = patternStage.getResult();
		if (changedItem == null) {
			// User cancelled/closed the window
			return;
		}
		final Machine machine = currentProject.getCurrentMachine();
		LTLPatternParser.removePattern(oldItem, machine);
		if(machine.getMachineProperties().getLTLPatterns().stream().noneMatch(existing -> !existing.settingsEqual(oldItem) && existing.settingsEqual(changedItem))) {
			machine.getMachineProperties().getLTLPatterns().set(machine.getMachineProperties().getLTLPatterns().indexOf(oldItem), changedItem);
			LTLPatternParser.addPattern(changedItem, machine);
			currentProject.setSaved(false); // FIXME Does this really need to be set manually?
		} else {
			stageManager.makeAlert(Alert.AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content.pattern").show();
		}
	}
	
	@FXML
	private void saveLTL() {
		Machine machine = currentProject.getCurrentMachine();
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("verifications.temporal.temporalView.fileChooser.saveLTL.title"));
		fileChooser.setInitialFileName(machine.getName() + "." + LTL_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.ltl", LTL_FILE_EXTENSION));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if (path != null) {
			List<TemporalFormulaItem> formulas = itemsTable.getItems().stream()
				.filter(item -> item.getType() == TemporalFormulaType.LTL)
				.filter(TemporalFormulaItem::selected)
				.collect(Collectors.toList());
			List<LTLPatternItem> patterns = machine.getMachineProperties().getLTLPatterns();
			try {
				final JsonMetadata metadata = LTLData.metadataBuilder()
					.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
					.withModelName(machine.getName())
					.build();
				this.jacksonManager.writeToFile(path, new LTLData(formulas, patterns, metadata));
			} catch (IOException e) {
				final Alert alert = stageManager.makeExceptionAlert(e, "verifications.temporal.temporalView.saveLTL.error");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		}
	}

	@FXML
	private void loadLTL() {
		Machine machine = currentProject.getCurrentMachine();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("verifications.temporal.temporalView.fileChooser.loadLTL.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.ltl", LTL_FILE_EXTENSION, OLD_LTL_FILE_EXTENSION));
		Path ltlFile = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if(ltlFile == null) {
			return;
		}
		LTLData data;
		try {
			data = this.jacksonManager.readFromFile(ltlFile);
		} catch (IOException e) {
			LOGGER.error("Could not load LTL file: ", e);
			return;
		}
		data.getFormulas().forEach(this::addItem);
		data.getPatterns().stream()
				.filter(pattern -> !machine.getMachineProperties().getLTLPatterns().contains(pattern))
				.forEach(pattern -> {
					machine.getMachineProperties().getLTLPatterns().add(pattern);
					LTLPatternParser.addPattern(pattern, machine);
				});
	}
}
