package de.prob2.ui.operations;

import java.util.List;
import java.util.Objects;

import com.google.inject.Inject;

import de.prob.prolog.term.PrologTerm;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;

public class OperationDetailsStage extends Stage {
	private static final class ValueItem {
		private final String name;
		private final String value;
		
		ValueItem(final String name, final String value) {
			super();
			
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(value, "value");
			
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	@FXML private TreeTableView<ValueItem> valuesTreeView;
	@FXML private TreeTableColumn<ValueItem, String> nameColumn;
	@FXML private TreeTableColumn<ValueItem, String> valueColumn;
	@FXML private TreeItem<ValueItem> rootItem;
	@FXML private TreeItem<ValueItem> parametersItem;
	@FXML private TreeItem<ValueItem> returnValuesItem;
	@FXML private TreeItem<ValueItem> constantsItem;
	@FXML private TreeItem<ValueItem> variablesItem;
	@FXML private TreeItem<ValueItem> transitionInfosItem;
	@FXML private TextArea textArea;
	
	private final I18n i18n;
	private final CliTaskExecutor cliExecutor;
	
	private final ObjectProperty<OperationItem> item;
	
	@Inject
	OperationDetailsStage(StageManager stageManager, I18n i18n, CliTaskExecutor cliExecutor) {
		super();
		
		this.i18n = i18n;
		this.cliExecutor = cliExecutor;

		this.item = new SimpleObjectProperty<>(this, "item", null);
		
		stageManager.loadFXML(this, "operation_details_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.valuesTreeView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.textArea.setText(to == null ? null : to.getValue().getValue()));
		
		this.nameColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getValue().getName()));
		this.valueColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getValue().getValue()));
		
		this.rootItem.setValue(new ValueItem("Values (this root item should be invisible)", ""));
		this.parametersItem.setValue(new ValueItem(i18n.translate("operations.operationDetails.groups.parameters"), ""));
		this.returnValuesItem.setValue(new ValueItem(i18n.translate("operations.operationDetails.groups.returnValues"), ""));
		this.constantsItem.setValue(new ValueItem(i18n.translate("operations.operationDetails.groups.constants"), ""));
		this.variablesItem.setValue(new ValueItem(i18n.translate("operations.operationDetails.groups.variables"), ""));
		this.transitionInfosItem.setValue(new ValueItem(i18n.translate("operations.operationDetails.groups.transitionInfos"), ""));
		
		this.item.addListener((observable, from, to) -> {
			this.rootItem.getChildren().clear();
			if (to == null) {
				this.setTitle(i18n.translate("operations.operationDetails.title"));
			} else {
				this.setTitle(i18n.translate("operations.operationDetails.titleWithName", to.getPrettyName()));
			}

			if (to != null && to.getTransition() != null) {
				this.cliExecutor.execute(() -> {
					// get information about all variables/constants
					// they are not queried by default - see HistoryItem#itemsForTrace
					OperationItem target = OperationItem.forTransition(to.getTransition().getStateSpace(), to.getTransition());
					Platform.runLater(() -> this.update(target));
				});
			} else {
				this.update(to);
			}
		});
	}

	private void update(OperationItem to) {
		this.rootItem.getChildren().add(this.parametersItem);
		this.rootItem.getChildren().add(this.returnValuesItem);
		this.rootItem.getChildren().add(this.constantsItem);
		this.rootItem.getChildren().add(this.variablesItem);
		this.rootItem.getChildren().add(this.transitionInfosItem);
		this.rootItem.getChildren().forEach(ti -> ti.getChildren().clear());
		if (to != null) {
			final List<String> paramNames = to.getParameterNames();
			final List<String> paramValues = to.getParameterValues();
			if (paramValues.isEmpty()) {
				for (final String name : paramNames) {
					this.parametersItem.getChildren().add(new TreeItem<>(new ValueItem(name, "")));
				}
			} else {
				for (int i = 0; i < paramValues.size(); i++) {
					String name = i < paramNames.size() ? paramNames.get(i) : "#" + (i + 1);
					final String param = paramValues.get(i);
					this.parametersItem.getChildren().add(new TreeItem<>(new ValueItem(name, param)));
				}
			}

			final List<String> returnNames = to.getReturnParameterNames();
			final List<String> returnValues = to.getReturnParameterValues();
			for (int i = 0; i < returnValues.size(); i++) {
				final String name = i < returnNames.size() ? returnNames.get(i) : "#" + (i + 1);
				final String retval = returnValues.get(i);
				this.returnValuesItem.getChildren().add(new TreeItem<>(new ValueItem(name, retval)));
			}

			to.getConstants().forEach((key, value) -> this.constantsItem.getChildren().add(new TreeItem<>(new ValueItem(key, value))));

			to.getVariables().forEach((key, value) -> this.variablesItem.getChildren().add(new TreeItem<>(new ValueItem(key, value))));

			to.getTransitionInfos().forEach((key,value) -> {
				if (value.isEmpty()) {
					this.transitionInfosItem.getChildren().add(new TreeItem<>(new ValueItem(key, "")));
					return;
				}
				for (PrologTerm pt : value) {
					this.transitionInfosItem.getChildren().add(new TreeItem<>(new ValueItem(key, pt.getFunctor())));
				}
			});
		}
		this.rootItem.getChildren().removeIf(ti -> ti.getChildren().isEmpty());
	}

	public ObjectProperty<OperationItem> itemProperty() {
		return item;
	}
	
	public OperationItem getItem() {
		return this.itemProperty().get();
	}
	
	public void setItem(final OperationItem item) {
		this.itemProperty().set(item);
	}
}
