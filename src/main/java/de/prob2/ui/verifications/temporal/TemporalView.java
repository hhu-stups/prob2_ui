package de.prob2.ui.verifications.temporal;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternParser;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternStage;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
@Singleton
public final class TemporalView extends CheckingViewBase<TemporalFormulaItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("verifications.temporal.temporalView.contextMenu.check"));
			
			MenuItem showCounterExampleItem = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showCounterExample"));
			showCounterExampleItem.setOnAction(e -> currentTrace.set(itemsTable.getSelectionModel().getSelectedItem().getCounterExample()));
			showCounterExampleItem.setDisable(true);
			contextMenu.getItems().add(showCounterExampleItem);
			
			MenuItem showMessage = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResult().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);
			
			this.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultProperty().isNull());
					showCounterExampleItem.disableProperty().bind(to.counterExampleProperty().isNull());
				}
			});
		}
	}
	
	@FXML 
	private MenuButton addMenuButton;
	@FXML
	private MenuItem addFormulaButton;
	@FXML
	private MenuItem addPatternButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	@FXML
	private TableColumn<TemporalFormulaItem, String> formulaDescriptionColumn;
	@FXML
	private TableColumn<LTLPatternItem, CheckingStatus> patternStatusColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;

	private final StageManager stageManager;
	private final I18n i18n;
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;

	@Inject
	private TemporalView(final StageManager stageManager, final I18n i18n, final Injector injector,
						 final CurrentTrace currentTrace, final CurrentProject currentProject,
						 final DisablePropertyController disablePropertyController,
						 final CliTaskExecutor cliExecutor) {
		super(i18n, disablePropertyController, currentTrace, currentProject, cliExecutor);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "temporal_view.fxml");
	}

	@Override
	protected ObservableList<TemporalFormulaItem> getItemsProperty(Machine machine) {
		return machine.getTemporalFormulas();
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
			if (to != null) {
				tvPattern.itemsProperty().bind(to.getLTLPatterns());
				managePatternTable(to.getLTLPatterns());
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
				machine.getLTLPatterns().remove(item);
				LTLPatternParser.removePattern(item, machine);
				managePatternTable(machine.getLTLPatterns());
			});

			MenuItem openEditor = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.edit"));
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));
			
			MenuItem showMessage = new MenuItem(i18n.translate("verifications.temporal.temporalView.contextMenu.showParsingMessage"));
			showMessage.setOnAction(e -> row.getItem().getResult().showAlert(stageManager, i18n));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultProperty().isNull());
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
		patternStatusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		patternColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		addMenuButton.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));

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
		if (machine.getLTLPatterns().stream().noneMatch(newItem::settingsEqual)) {
			LTLPatternParser.addPattern(newItem, machine);
			machine.getLTLPatterns().add(newItem);
			managePatternTable(machine.getLTLPatterns());
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
		if(machine.getLTLPatterns().stream().noneMatch(existing -> !existing.settingsEqual(oldItem) && existing.settingsEqual(changedItem))) {
			machine.getLTLPatterns().set(machine.getLTLPatterns().indexOf(oldItem), changedItem);
			LTLPatternParser.addPattern(changedItem, machine);
			currentProject.setSaved(false); // FIXME Does this really need to be set manually?
		} else {
			stageManager.makeAlert(Alert.AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content.pattern").show();
		}
	}
}
