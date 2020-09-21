package de.prob2.ui.verifications.ltl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.ObjectWithMetadata;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaStage;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternStage;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class LTLView extends AnchorPane {
	private static final Logger logger = LoggerFactory.getLogger(LTLView.class);
	
	private static final String LTL_FILE_EXTENSION = "prob2ltl";
	private static final String OLD_LTL_FILE_EXTENSION = "ltl";
	
	@FXML 
	private MenuButton addMenuButton;
	@FXML
	private MenuItem addFormulaButton;
	@FXML
	private MenuItem addPatternButton;
	@FXML
	private Button checkMachineButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button saveLTLButton;
	@FXML
	private Button loadLTLButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	@FXML
	private TableColumn<IExecutableItem, CheckBox> formulaSelectedColumn;
	@FXML
	private TableColumn<LTLFormulaItem, Checked> formulaStatusColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaDescriptionColumn;
	@FXML
	private TableColumn<LTLPatternItem, Checked> patternStatusColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final VersionInfo versionInfo;
	private final CurrentProject currentProject;
	private final LTLFormulaChecker checker;
	private final LTLPatternParser patternParser;
	private final LTLResultHandler resultHandler;
	private final FileChooserManager fileChooserManager;
	private final CheckBox formulaSelectAll;
	private final JsonManager<LTLData> jsonManager;
				
	@Inject
	private LTLView(final StageManager stageManager, final ResourceBundle bundle, final Injector injector,
					final CurrentTrace currentTrace, final VersionInfo versionInfo, final CurrentProject currentProject,
					final LTLFormulaChecker checker, final LTLPatternParser patternParser,
					final LTLResultHandler resultHandler,
					final FileChooserManager fileChooserManager,
					final Gson gson,
					final JsonManager<LTLData> jsonManager) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.versionInfo = versionInfo;
		this.currentProject = currentProject;
		this.checker = checker;
		this.patternParser = patternParser;
		this.resultHandler = resultHandler;
		this.fileChooserManager = fileChooserManager;
		this.jsonManager = jsonManager;
		jsonManager.initContext(new JsonManager.Context<LTLData>(gson, LTLData.class, "LTL", 1) {
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFileType() == null) {
					assert oldMetadata.getFormatVersion() == 0;
					for (final String fieldName : new String[] {"formulas", "patterns"}) {
						if (!oldObject.has(fieldName)) {
							throw new JsonParseException("Not a valid LTL file - missing required field " + fieldName);
						}
					}
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
		this.formulaSelectAll = new CheckBox();
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("verification", "LTL");
		setOnItemClicked();
		setContextMenus();
		setBindings();
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if(to != null) {
				if(from != null) {
					from.clearPatternManager();
				}
				tvFormula.itemsProperty().bind(to.ltlFormulasProperty());
				tvPattern.itemsProperty().bind(to.ltlPatternsProperty());
			} else {
				tvFormula.getItems().clear();
				tvFormula.itemsProperty().unbind();
				tvPattern.getItems().clear();
				tvPattern.itemsProperty().unbind();
			}
		});
	}
	
	private void setOnItemClicked() {
		tvFormula.setOnMouseClicked(e-> {
			LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null) {
				checker.checkFormula(item);
			}
		});
	}

	/**
	 * Sets the context menus for the items LTLFormula and LTLPatterns
	 */
	private void setContextMenus() {
		tvFormula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.removeFormula"));
			removeItem.setOnAction(e -> removeFormula());
						
			MenuItem showCounterExampleItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.showCounterExample"));
			showCounterExampleItem.setOnAction(e-> currentTrace.set(tvFormula.getSelectionModel().getSelectedItem().getCounterExample()));
			showCounterExampleItem.setDisable(true);

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.openInEditor"));
			openEditor.setOnAction(e->showCurrentItemDialog(row.getItem()));
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> resultHandler.showResult(row.getItem()));

			MenuItem checkItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				checker.checkFormula(row.getItem());
			});
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(checker.runningProperty().or(to.selectedProperty().not()));
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showCounterExampleItem.disableProperty().bind(to.counterExampleProperty().isNull());
				}
			});
			
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(checkItem, openEditor, removeItem, showCounterExampleItem, showMessage)));
			return row;
		});
		
		tvPattern.setRowFactory(table -> {
			final TableRow<LTLPatternItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.removePattern"));
			removeItem.setOnAction(e -> removePattern());

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.openInEditor"));
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.showParsingMessage"));
			showMessage.setOnAction(e -> resultHandler.showResult(row.getItem()));
			
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

	private void setBindings() {
		formulaSelectedColumn.setCellValueFactory(new ItemSelectedFactory(tvFormula, formulaSelectAll));
		formulaSelectedColumn.setGraphic(formulaSelectAll);
		formulaStatusColumn.setCellFactory(col -> new CheckedCell<>());
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		formulaColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		patternStatusColumn.setCellFactory(col -> new CheckedCell<>());
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		patternColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		addMenuButton.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		cancelButton.disableProperty().bind(checker.runningProperty().not());
		final BooleanProperty noLtlFormulas = new SimpleBooleanProperty();
		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			if (to != null) {
				noLtlFormulas.bind(to.ltlFormulasProperty().emptyProperty());
			} else {
				noLtlFormulas.unbind();
				noLtlFormulas.set(true);
			}
		});
		checkMachineButton.disableProperty().bind(currentTrace.isNull().or(noLtlFormulas.or(formulaSelectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()))));
		saveLTLButton.disableProperty().bind(noLtlFormulas.or(currentTrace.isNull().or(formulaSelectAll.selectedProperty().not())));
		loadLTLButton.disableProperty().bind(currentTrace.isNull());

		tvFormula.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
	}
	
	@FXML
	public void addFormula() {
		LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
		loadLTLStage(formulaStage, null);
		formulaStage.setHandleItem(new LTLHandleItem<>(HandleType.ADD, null));
		formulaStage.showAndWait();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.getLTLFormulas().remove(item);
	}
	
	@FXML
	public void addPattern() {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		loadLTLStage(patternStage, null);
		patternStage.setHandleItem(new LTLHandleItem<>(LTLHandleItem.HandleType.ADD, null));
		patternStage.showAndWait();
	}
	
	private void removePattern() {
		Machine machine = currentProject.getCurrentMachine();
		LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
		machine.getLTLPatterns().remove(item);
		patternParser.removePattern(item, machine);
	}
	
	private void showCurrentItemDialog(LTLFormulaItem item) {
		LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
		loadLTLStage(formulaStage, item);
		formulaStage.setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
		formulaStage.showAndWait();
		formulaStage.clear();
	}
	
	private void showCurrentItemDialog(LTLPatternItem item) {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		loadLTLStage(patternStage, item);
		patternStage.setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
		patternStage.showAndWait();
		patternStage.clear();
	}

	private void loadLTLStage(LTLItemStage<?> stage, AbstractCheckableItem item) {
		stage.getEngine().getLoadWorker().stateProperty().addListener((observable, from, to) -> {
			if(to == Worker.State.SUCCEEDED && item != null) {
				stage.setData(item.getDescription(), item.getCode());
			}
		});
	}
	
	@FXML
	public void checkMachine() {
		checker.checkMachine();
	}
	
	@FXML
	public void cancel() {
		checker.cancel();
	}
	
	@FXML
	private void saveLTL() {
		Machine machine = currentProject.getCurrentMachine();
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.ltl.ltlView.fileChooser.saveLTL.title"));
		fileChooser.setInitialFileName(machine.getName() + "." + LTL_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.ltl", LTL_FILE_EXTENSION));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if (path != null) {
			List<LTLFormulaItem> formulas = machine.getLTLFormulas().stream()
				.filter(LTLFormulaItem::selected)
				.collect(Collectors.toList());
			List<LTLPatternItem> patterns = machine.getLTLPatterns().stream()
				.filter(LTLPatternItem::selected)
				.collect(Collectors.toList());
			try {
				final JsonMetadata metadata = this.jsonManager.defaultMetadataBuilder()
					.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
					.withModelName(machine.getName())
					.build();
				this.jsonManager.writeToFile(path, new LTLData(formulas, patterns), metadata);
			} catch (IOException e) {
				stageManager.makeExceptionAlert(e, "verifications.ltl.ltlView.saveLTL.error").showAndWait();
			}
		}
	}

	@FXML
	private void loadLTL() {
		Machine machine = currentProject.getCurrentMachine();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.ltl.ltlView.fileChooser.loadLTL.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.ltl", LTL_FILE_EXTENSION, OLD_LTL_FILE_EXTENSION));
		Path ltlFile = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if(ltlFile == null) {
			return;
		}
		LTLData data;
		try {
			data = this.jsonManager.readFromFile(ltlFile).getObject();
		} catch (IOException e) {
			logger.error("Could not load LTL file: ", e);
			return;
		}
		data.getFormulas().stream()
				.filter(formula -> !machine.getLTLFormulas().contains(formula))
				.forEach(machine.getLTLFormulas()::add);
		data.getPatterns().stream()
				.filter(pattern -> !machine.getLTLPatterns().contains(pattern))
				.forEach(pattern -> {
					machine.getLTLPatterns().add(pattern);
					patternParser.addPattern(pattern, machine);
				});
	}
}
