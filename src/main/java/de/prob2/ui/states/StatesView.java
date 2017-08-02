package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.command.GetMachineStructureCommand;
import de.prob.animator.domainobjects.*;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import de.prob.animator.prologast.PrologASTNode;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
public final class StatesView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatesView.class);

	@FXML private TreeTableView<StateItem<?>> tv;
	@FXML private TreeTableColumn<StateItem<?>, StateItem<?>> tvName;
	@FXML private TreeTableColumn<StateItem<?>, StateItem<?>> tvValue;
	@FXML private TreeTableColumn<StateItem<?>, StateItem<?>> tvPreviousValue;
	@FXML private TreeItem<StateItem<?>> tvRootItem;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final FormulaGenerator formulaGenerator;
	private final StageManager stageManager;

	private final Map<IEvalElement, AbstractEvalResult> currentValues;
	private final Map<IEvalElement, AbstractEvalResult> previousValues;

	@Inject
	private StatesView(
		final Injector injector,
		final CurrentTrace currentTrace,
		final FormulaGenerator formulaGenerator,
		final StageManager stageManager
	) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.formulaGenerator = formulaGenerator;
		this.stageManager = stageManager;

		this.currentValues = new HashMap<>();
		this.previousValues = new HashMap<>();

		stageManager.loadFXML(this, "states_view.fxml");
	}
	
	@FXML
	private void initialize() {
		tv.setRowFactory(view -> {
			final TreeTableRow<StateItem<?>> row = new TreeTableRow<>();

			row.itemProperty().addListener((observable, from, to) -> {
				row.getStyleClass().remove("changed");
				if (to != null && to.getContents() instanceof ASTFormula) {
					final IEvalElement formula = ((ASTFormula)to.getContents()).getFormula();
					final AbstractEvalResult current = this.currentValues.get(formula);
					final AbstractEvalResult previous = this.previousValues.get(formula);

					if (current != null && previous != null && (
						!current.getClass().equals(previous.getClass())
						|| current instanceof EvalResult && !((EvalResult)current).getValue().equals(((EvalResult)previous).getValue())
					)) {
						row.getStyleClass().add("changed");
					}
				}
			});

			final MenuItem visualizeExpressionItem = new MenuItem("Visualize Expression");
			// Expression can only be shown if the row item contains an ASTFormula and the current state is initialized.
			visualizeExpressionItem.disableProperty().bind(
				Bindings.createBooleanBinding(() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not())
			);
			visualizeExpressionItem.setOnAction(event -> {
				try {
					formulaGenerator.showFormula(((ASTFormula)row.getItem().getContents()).getFormula());
				} catch (EvaluationException | ProBError e) {
					LOGGER.error("Could not visualize formula", e);
					stageManager.makeAlert(Alert.AlertType.ERROR, "Could not visualize formula:\n" + e).showAndWait();
				}
			});
			
			final MenuItem showFullValueItem = new MenuItem("Show Full Value");
			// Full value can only be shown if the row item contains any of the following:
			// * An ASTFormula, and the corresponding value is an EvalResult.
			// * A StateError
			showFullValueItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(
					row.getItem().getContents() instanceof ASTFormula
					&& this.currentValues.get(((ASTFormula)row.getItem().getContents()).getFormula()) instanceof EvalResult
					|| row.getItem().getContents() instanceof StateError
				),
				row.itemProperty()
			));
			showFullValueItem.setOnAction(event -> this.showFullValue(row.getItem()));

			final MenuItem showErrorsItem = new MenuItem("Show Errors");
			// Errors can only be shown if the row contains an ASTFormula whose value is an EvaluationErrorResult.
			showErrorsItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(
					row.getItem().getContents() instanceof ASTFormula
					&& this.currentValues.get(((ASTFormula)row.getItem().getContents()).getFormula()) instanceof EvaluationErrorResult
				),
				row.itemProperty()
			));
			showErrorsItem.setOnAction(event -> this.showError(row.getItem()));

			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu) null)
				.otherwise(new ContextMenu(visualizeExpressionItem, showFullValueItem, showErrorsItem))
			);

			// Double-click on an item triggers "show full value" if allowed.
			row.setOnMouseClicked(event -> {
				if (!showFullValueItem.isDisable() && event.getButton() == MouseButton.PRIMARY
						&& event.getClickCount() == 2) {
					showFullValueItem.getOnAction().handle(null);
				}
			});

			return row;
		});

		this.tvName.setCellFactory(col -> new NameCell());
		this.tvValue.setCellFactory(col -> new ValueCell(this.currentValues, true));
		this.tvPreviousValue.setCellFactory(col -> new ValueCell(this.previousValues, false));

		final Callback<TreeTableColumn.CellDataFeatures<StateItem<?>, StateItem<?>>, ObservableValue<StateItem<?>>> cellValueFactory = data -> Bindings.createObjectBinding(data.getValue()::getValue, this.currentTrace);
		this.tvName.setCellValueFactory(cellValueFactory);
		this.tvValue.setCellValueFactory(cellValueFactory);
		this.tvPreviousValue.setCellValueFactory(cellValueFactory);

		this.tvRootItem.setValue(new StateItem<>("Machine (this root item should be invisible)", false));

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
	
	private static boolean isSameNode(final PrologASTNode x, final PrologASTNode y) {
		final boolean isSameCategory = x instanceof ASTCategory && y instanceof ASTCategory && ((ASTCategory)x).getName().equals(((ASTCategory)y).getName());
		final boolean isSameFormula = x instanceof ASTFormula && y instanceof ASTFormula && ((ASTFormula)x).getFormula().equals(((ASTFormula)y).getFormula());
		return isSameCategory || isSameFormula;
	}

	private static void getFormulasToSubscribe(final List<PrologASTNode> nodes, final List<IEvalElement> formulas) {
		for (final PrologASTNode node : nodes) {
			if (node instanceof ASTFormula) {
				formulas.add(((ASTFormula)node).getFormula());
			}
			
			getFormulasToSubscribe(node.getSubnodes(), formulas);
		}
	}
	
	private static List<IEvalElement> getFormulasToSubscribe(final List<PrologASTNode> nodes) {
		final List<IEvalElement> formulas = new ArrayList<>();
		getFormulasToSubscribe(nodes, formulas);
		return formulas;
	}

	private static void updateNodes(final TreeItem<StateItem<?>> treeItem, final List<PrologASTNode> nodes) {
		Objects.requireNonNull(treeItem);
		Objects.requireNonNull(nodes);
		
		final Set<TreeItem<StateItem<?>>> toRemove = new HashSet<>(treeItem.getChildren());
		for (final PrologASTNode node : nodes) {
			TreeItem<StateItem<?>> subTreeItem = null;
			for (final TreeItem<StateItem<?>> it : treeItem.getChildren()) {
				final Object contents = it.getValue().getContents();
				if (contents instanceof PrologASTNode && isSameNode((PrologASTNode)contents, node)) {
					subTreeItem = it;
					toRemove.remove(subTreeItem);
					break;
				}
			}
			if (subTreeItem == null) {
				subTreeItem = new TreeItem<>();
				if (node instanceof ASTCategory && ((ASTCategory)node).isExpanded()) {
					subTreeItem.setExpanded(true);
				}
				treeItem.getChildren().add(subTreeItem);
			}
			
			subTreeItem.setValue(new StateItem<>(node, false));
			updateNodes(subTreeItem, node.getSubnodes());
		}
		// Remove all TreeItems for which no node was found anymore in the nodes list.
		treeItem.getChildren().removeAll(toRemove);
	}

	private void updateRoot(final Trace trace) {
		Objects.requireNonNull(trace);
		
		final int row = tv.getSelectionModel().getSelectedIndex();
		
		final TreeItem<StateItem<?>> errorsItem;
		if (this.tvRootItem.getChildren().isEmpty()) {
			errorsItem = new TreeItem<>();
			errorsItem.setExpanded(true);
		} else {
			errorsItem = this.tvRootItem.getChildren().remove(this.tvRootItem.getChildren().size()-1);
			assert "State Errors".equals(errorsItem.getValue().getContents());
		}
		
		final GetMachineStructureCommand cmd = new GetMachineStructureCommand();
		trace.getStateSpace().execute(cmd);
		final List<PrologASTNode> rootNodes = cmd.getPrologASTList();
		
		trace.getStateSpace().subscribe(this, getFormulasToSubscribe(rootNodes));
		
		this.currentValues.clear();
		this.currentValues.putAll(trace.getCurrentState().getValues());
		this.previousValues.clear();
		if (trace.canGoBack()) {
			this.previousValues.putAll(trace.getPreviousState().getValues());
		}
		
		updateNodes(this.tvRootItem, rootNodes);

		errorsItem.getChildren().clear();
		for (final StateError error : trace.getCurrentState().getStateErrors()) {
			errorsItem.getChildren().add(new TreeItem<>(new StateItem<>(error, true)));
		}

		errorsItem.setValue(new StateItem<>("State Errors", !errorsItem.getChildren().isEmpty()));
		this.tvRootItem.getChildren().add(errorsItem);

		this.tv.refresh();
		this.tv.getSelectionModel().select(row);
	}
	
	private void showError(StateItem<?> stateItem) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		if (stateItem.getContents() instanceof ASTFormula) {
			final AbstractEvalResult result = this.currentValues.get(((ASTFormula)stateItem.getContents()).getFormula());
			if (result instanceof EvaluationErrorResult) {
				stage.setTitle(stateItem.toString());
				stage.setCurrentValue(String.join("\n", ((EvaluationErrorResult) result).getErrors()));
				stage.setFormattingEnabled(false);
				stage.show();
			} else {
				throw new IllegalArgumentException("Row item result is not an error: " + result.getClass());
			}
		} else {
			throw new IllegalArgumentException("Invalid row item type: " + stateItem.getClass());
		}
	}

	private static String getResultValue(final ASTFormula element, final State state) {
		final AbstractEvalResult result = state.eval(element.getFormula(FormulaExpand.expand));
		return result instanceof EvalResult ? ((EvalResult)result).getValue() : null;
	}

	private void showFullValue(StateItem<?> stateItem) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		if (stateItem.getContents() instanceof ASTFormula) {
			final ASTFormula element = (ASTFormula)stateItem.getContents();
			stage.setTitle(element.getFormula().toString());
			stage.setCurrentValue(getResultValue(element, this.currentTrace.getCurrentState()));
			stage.setPreviousValue(getResultValue(element, this.currentTrace.get().getPreviousState()));
			stage.setFormattingEnabled(true);
		} else if (stateItem.getContents() instanceof StateError) {
			final StateError error = (StateError) stateItem.getContents();
			stage.setTitle(error.getEvent());
			stage.setCurrentValue(error.getLongDescription());
			stage.setPreviousValue(null);
			stage.setFormattingEnabled(false);
		} else {
			throw new IllegalArgumentException("Invalid row item type: " + stateItem.getClass());
		}
		stage.show();
	}

	public TreeTableView<StateItem<?>> getTable() {
		return tv;
	}
}
