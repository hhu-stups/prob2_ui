package de.prob2.ui.states;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.Machine;
import de.prob.statespace.Trace;

import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StatesView extends AnchorPane {
	private static final class ValueCell extends TreeTableCell<StateTreeItem<?>, String> {
		private ValueCell() {
			super();
		}
		
		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (item == null || empty) {
				super.setText(null);
				super.setGraphic(null);
				this.getStyleClass().removeAll("false", "true");
			} else {
				
				super.setText(item);
				super.setGraphic(null);
				
				if ("FALSE".equals(item)) {
					if (!this.getStyleClass().contains("false")) {
						this.getStyleClass().add("false");
					}
				} else {
					this.getStyleClass().remove("false");
				}
				
				if ("TRUE".equals(item)) {
					if (!this.getStyleClass().contains("true")) {
						this.getStyleClass().add("true");
					}
				} else {
					this.getStyleClass().remove("true");
				}
			}
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(StatesView.class);
	
	@FXML private TreeTableView<StateTreeItem<?>> tv;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvName;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvValue;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvPreviousValue;
	@FXML private TreeItem<StateTreeItem<?>> tvRootItem;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final ClassBlacklist classBlacklist;
	private final FormulaGenerator formulaGenerator;
	private final StageManager stageManager;

	private Map<IEvalElement, AbstractEvalResult> currentValues;
	private Map<IEvalElement, AbstractEvalResult> previousValues;

	@Inject
	private StatesView(
		final Injector injector,
		final CurrentTrace currentTrace,
		final ClassBlacklist classBlacklist,
		final FormulaGenerator formulaGenerator,
		final StageManager stageManager
	) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.classBlacklist = classBlacklist;
		this.formulaGenerator = formulaGenerator;
		this.stageManager = stageManager;

		stageManager.loadFXML(this, "states_view.fxml");
	}

	private void unsubscribeAllChildren(final Trace trace, final AbstractElement element) {
		if (element instanceof AbstractFormulaElement) {
			((AbstractFormulaElement) element).unsubscribe(trace.getStateSpace());
		}
		
		for (final Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			this.unsubscribeAllChildren(trace, element.getChildren().get(clazz));
		}
	}

	private void unsubscribeAllChildren(final Trace trace, final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			this.unsubscribeAllChildren(trace, e);
		}
	}

	private void updateElements(final Trace trace, final TreeItem<StateTreeItem<?>> treeItem,
			final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			if (e instanceof AbstractFormulaElement) {
				((AbstractFormulaElement) e).subscribe(trace.getStateSpace());
			}
		}

		this.currentValues = trace.getCurrentState().getValues();
		this.previousValues = trace.canGoBack() ? trace.getPreviousState().getValues() : null;

		for (AbstractElement e : elements) {
			TreeItem<StateTreeItem<?>> childItem = null;

			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();
				if (sti.getContents().equals(e)) {
					childItem = ti;
					childItem.setValue(ElementStateTreeItem.fromElementAndValues(e, this.currentValues, this.previousValues));
					
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(ElementStateTreeItem.fromElementAndValues(e, this.currentValues, this.previousValues));
				treeItem.getChildren().add(childItem);
			}

			this.updateChildren(trace, childItem, e);
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (sti instanceof ElementStateTreeItem) {
				AbstractElement element = ((ElementStateTreeItem) sti).getContents();
				if (!elements.contains(element)) {
					it.remove();
					if (element instanceof AbstractFormulaElement) {
						((AbstractFormulaElement) element).unsubscribe(trace.getStateSpace());
					}
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort(Comparator.comparing(a -> a.getValue().getName()));
	}

	private void updateChildren(final Trace trace, final TreeItem<StateTreeItem<?>> treeItem,
			final AbstractElement element) {
		this.classBlacklist.getKnownClasses().addAll(element.getChildren().keySet());
		for (Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			if (this.classBlacklist.getBlacklist().contains(clazz)) {
				continue;
			}

			TreeItem<StateTreeItem<?>> childItem = null;
			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();

				if (sti.getContents().equals(clazz)) {
					childItem = ti;
					break;
				}

			}

			if (childItem == null) {
				childItem = new TreeItem<>(new ElementClassStateTreeItem(clazz));
				treeItem.getChildren().add(childItem);
			}

			this.updateElements(trace, childItem, element.getChildren().get(clazz));
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (sti instanceof ElementClassStateTreeItem) {
				Class<? extends AbstractElement> clazz = ((ElementClassStateTreeItem) sti).getContents();
				if (!element.getChildren().containsKey(clazz) || this.classBlacklist.getBlacklist().contains(clazz)) {
					this.unsubscribeAllChildren(trace, element.getChildren().get(clazz));
					it.remove();
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort(Comparator.comparing(a -> a.getValue().getName()));
	}

	private void updateRoot(final Trace trace) {
		this.updateElements(trace, this.tvRootItem, trace.getModel().getChildrenOfType(Machine.class));
	}

	private void visualizeExpression(AbstractFormulaElement formula) {
		try {
			formulaGenerator.showFormula(formula.getFormula());
		} catch (EvaluationException | ProBError e) {
			logger.error("Could not visualize formula", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not visualize formula:\n" + e).showAndWait();
		}
	}

	@FXML
	public void initialize() {
		this.currentValues = null;
		this.previousValues = null;

		tv.setRowFactory(view -> { // NOSONAR // Sonar counts every if statement in a lambda as a conditional expression and complains if there are more than 3. This is not a reasonable limit here.
			final TreeTableRow<StateTreeItem<?>> row = new TreeTableRow<>();
			
			row.itemProperty().addListener((observable, from, to) -> {
				if (
					!(to instanceof ElementStateTreeItem)
					|| to.getValue() == null
					|| to.getPreviousValue() == null
					|| to.getValue().equals(to.getPreviousValue())
				) {
					row.getStyleClass().remove("changed");
				} else if (!row.getStyleClass().contains("changed")) {
					row.getStyleClass().add("changed");
				}
			});
			
			final MenuItem visualizeExpressionItem = new MenuItem("Visualize Expression");
			// Expression can only be shown if the row item is an ElementStateTreeItem containing an AbstractFormulaElement and the current state is initialized.
			visualizeExpressionItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> !(
					row.getItem() instanceof ElementStateTreeItem
					&& row.getItem().getContents() instanceof AbstractFormulaElement
				),
					row.itemProperty()
				).or(currentTrace.currentStateProperty().initializedProperty().not())
			);
			visualizeExpressionItem.setOnAction(event ->
				visualizeExpression((AbstractFormulaElement) ((ElementStateTreeItem) row.getItem()).getContents())
			);
			
			final MenuItem showFullValueItem = new MenuItem("Show Full Value");
			// Full value can only be shown if the row item is an ElementStateTreeItem containing an AbstractFormulaElement and the corresponding value is an EvalResult.
			showFullValueItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> !(
					row.getItem() instanceof ElementStateTreeItem
					&& row.getItem().getContents() instanceof AbstractFormulaElement
					&& this.currentValues.get(((AbstractFormulaElement)((ElementStateTreeItem)row.getItem()).getContents()).getFormula()) instanceof EvalResult
				),
				row.itemProperty()
			));
			showFullValueItem.setOnAction(event -> {
				final AbstractFormulaElement element = (AbstractFormulaElement)((ElementStateTreeItem)row.getItem()).getContents();
				final EvalResult value = (EvalResult)this.currentValues.get(element.getFormula());
				final EvalResult previousValue;
				if (this.previousValues != null && this.previousValues.get(element.getFormula()) instanceof EvalResult) {
					previousValue = (EvalResult)this.previousValues.get(element.getFormula());
				} else {
					previousValue = null;
				}
				final FullValueStage stage = injector.getInstance(FullValueStage.class);
				stage.setTitle(element.toString());
				stage.setCurrentValue(AsciiUnicodeString.fromAscii(value.getValue()));
				stage.setPreviousValue(AsciiUnicodeString.fromAscii(previousValue == null ? "(not initialized)" : previousValue.getValue()));
				stage.show();
			});
			
			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu) null)
				.otherwise(new ContextMenu(visualizeExpressionItem, showFullValueItem))
			);
			
			// Double-click on an item triggers "show full value" if allowed.
			row.setOnMouseClicked(event -> {
				if (!showFullValueItem.isDisable() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					showFullValueItem.getOnAction().handle(null);
				}
			});
			
			return row;
		});
		
		this.tvValue.setCellFactory(col -> new ValueCell());
		this.tvPreviousValue.setCellFactory(col -> new ValueCell());

		this.tvRootItem.setValue(new ElementClassStateTreeItem(Machine.class));

		this.classBlacklist.getBlacklist()
				.addListener((SetChangeListener<? super Class<? extends AbstractElement>>) change -> {
					if (this.currentTrace.exists()) {
						this.updateRoot(this.currentTrace.get());
					}
				});

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			if (to == null) {
				this.tvRootItem.getChildren().clear();
			} else {
				this.updateRoot(to);
			}
		};
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);
	}
}
