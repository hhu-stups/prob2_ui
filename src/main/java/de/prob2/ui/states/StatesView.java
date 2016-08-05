package de.prob2.ui.states;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.Action;
import de.prob.model.representation.Machine;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob2.ui.formula.FormulaGenerator;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;

@Singleton
public class StatesView extends AnchorPane implements IAnimationChangeListener {
	@FXML private TreeTableView<StateTreeItem<?>> tv;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvName;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvValue;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvPreviousValue;
	@FXML private TreeItem<StateTreeItem<?>> tvRootItem;
	
	private final AnimationSelector animationSelector;
	private final ClassBlacklist classBlacklist;
	private final FormulaGenerator formulaGenerator;
	
	private Map<IEvalElement, AbstractEvalResult> currentValues;
	private Map<IEvalElement, AbstractEvalResult> previousValues;

	@Inject
	private StatesView(
		final AnimationSelector animationSelector,
		final ClassBlacklist classBlacklist,
		final FormulaGenerator formulaGenerator,
		final FXMLLoader loader
	) {
		this.animationSelector = animationSelector;
		animationSelector.registerAnimationChangeListener(this);
		this.classBlacklist = classBlacklist;
		this.formulaGenerator = formulaGenerator;
		
		loader.setLocation(getClass().getResource("states_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void unsubscribeAllChildren(final Trace trace, final AbstractElement element) {
		if (element instanceof AbstractFormulaElement) {
			((AbstractFormulaElement)element).unsubscribe(trace.getStateSpace());
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

	private void updateElements(
		final Trace trace,
		final TreeItem<StateTreeItem<?>> treeItem,
		final List<? extends AbstractElement> elements
	) {
		for (AbstractElement e : elements) {
			if (e instanceof AbstractFormulaElement) {
				((AbstractFormulaElement)e).subscribe(trace.getStateSpace());
			}

			TreeItem<StateTreeItem<?>> childItem = null;

			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();
				if (sti.getContents().equals(e)) {
					childItem = ti;
					childItem.getValue().update(this.currentValues, this.previousValues);
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(new ElementStateTreeItem(e, this.currentValues, this.previousValues));
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
						((AbstractFormulaElement)element).unsubscribe(trace.getStateSpace());
					}
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().getName().compareTo(b.getValue().getName()));
	}

	private void updateChildren(
		final Trace trace,
		final TreeItem<StateTreeItem<?>> treeItem,
		final AbstractElement element
	) {
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

		treeItem.getChildren().sort((a, b) -> a.getValue().getName().compareTo(b.getValue().getName()));
	}

	@Override
	public void traceChange(Trace trace, boolean currentAnimationChanged) {
		if (trace == null) {
			this.tvRootItem.getChildren().clear();
		} else {
			try {
				this.currentValues = trace.getCurrentState().getValues();
				this.previousValues = trace.canGoBack() ? trace.getPreviousState().getValues() : null;
				this.updateElements(trace, this.tvRootItem, trace.getModel().getChildrenOfType(Machine.class));
			} catch (Exception e) {
				// Otherwise the exception gets lost somewhere deep in a
				// ProB log file, without a traceback.
				e.printStackTrace();
			}
		}
	}

	@Override
	public void animatorStatus(boolean busy) {}
	
	public void showExpression(AbstractFormulaElement formula) {
		formulaGenerator.setFormula(formula.getFormula());
	}

	@FXML
	public void initialize() {
		this.animationSelector.registerAnimationChangeListener(this);

		this.currentValues = null;
		this.previousValues = null;
		
		tv.setRowFactory(view -> {
			final TreeTableRow<StateTreeItem<?>> row = new TreeTableRow<>();
			final MenuItem showExpressionItem = new MenuItem("Show Expression");
			showExpressionItem.setDisable(true);
			row.itemProperty().addListener((observable, from, to) -> {
				showExpressionItem.setDisable(to == null || !(to instanceof ElementStateTreeItem && to.getContents() instanceof AbstractFormulaElement));
			});
			showExpressionItem.setOnAction(event -> {
				showExpression((AbstractFormulaElement)((ElementStateTreeItem)row.getItem()).getContents());
			});
			row.setContextMenu(new ContextMenu(showExpressionItem));
			return row;
		});
		
		this.tvRootItem.setValue(new ElementClassStateTreeItem(Machine.class));
		
		this.classBlacklist.getBlacklist().addListener(
			(SetChangeListener<? super Class<? extends AbstractElement>>)change -> {
				if (this.animationSelector.getCurrentTrace() != null) {
					this.updateElements(
						this.animationSelector.getCurrentTrace(),
						this.tvRootItem,
						this.animationSelector.getCurrentTrace().getModel().getChildrenOfType(Machine.class)
					);
				}
			}
		);
		
		this.classBlacklist.getKnownClasses().add(Action.class);
		this.classBlacklist.getBlacklist().add(Action.class);
	}
}
