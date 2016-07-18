package de.prob2.ui.states;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;

public class StatesView extends AnchorPane implements IAnimationChangeListener {
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvName;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvValue;
	@FXML private TreeTableColumn<StateTreeItem<?>, String> tvPreviousValue;
	@FXML private TreeItem<StateTreeItem<?>> tvChildrenItem;
	@FXML private TreeTableView<ElementStateTreeItem> tv;
	
	private final ClassBlacklist classBlacklist;
	
	private Trace trace;
	private Map<IEvalElement, AbstractEvalResult> currentValues;
	private Map<IEvalElement, AbstractEvalResult> previousValues;
	private final AnimationSelector animations;

	@Inject
	public StatesView(
		final FXMLLoader loader,
		final AnimationSelector animations,
		final ClassBlacklist classBlacklist
	) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);
		
		this.classBlacklist = classBlacklist;
		
		loader.setLocation(getClass().getResource("states_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String stringRep(final AbstractEvalResult res) {
		if (res == null) {
			return "null";
		} else if (res instanceof IdentifierNotInitialised) {
			return "(not initialized)";
		} else if (res instanceof EvalResult) {
			return ((EvalResult) res).getValue();
		} else if (res instanceof EvaluationErrorResult) {
			return ((EvaluationErrorResult) res).getResult();
		} else if (res instanceof EnumerationWarning) {
			return "?(∞)";
		} else {
			return res.getClass() + " toString: " + res;
		}
	}

	static String formatClassName(Class<?> clazz, boolean plural) {
		String shortName = clazz.getSimpleName();
		if (plural) {
			if (shortName.endsWith("y")) {
				shortName = shortName.substring(0, shortName.length() - 1) + "ies";
			} else {
				shortName += "s";
			}
		}
		return shortName;
	}

	private void unsubscribeAllChildren(final AbstractElement element) {
		if (element instanceof AbstractFormulaElement) {
			((AbstractFormulaElement)element).unsubscribe(this.trace.getStateSpace());
		}

		for (final Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			this.unsubscribeAllChildren(element.getChildren().get(clazz));
		}
	}

	private void unsubscribeAllChildren(final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			this.unsubscribeAllChildren(e);
		}
	}

	private void updateElements(
		final TreeItem<StateTreeItem<?>> treeItem,
		final List<? extends AbstractElement> elements
	) {
		for (AbstractElement e : elements) {
			if (e instanceof AbstractFormulaElement) {
				((AbstractFormulaElement)e).subscribe(this.trace.getStateSpace());
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

			this.updateChildren(childItem, e);
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
						((AbstractFormulaElement)element).unsubscribe(this.trace.getStateSpace());
					}
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	private void updateChildren(
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

			this.updateElements(childItem, element.getChildren().get(clazz));
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (sti instanceof ElementClassStateTreeItem) {
				Class<? extends AbstractElement> clazz = ((ElementClassStateTreeItem) sti).getContents();
				if (!element.getChildren().containsKey(clazz) || this.classBlacklist.getBlacklist().contains(clazz)) {
					this.unsubscribeAllChildren(element.getChildren().get(clazz));
					it.remove();
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	@Override
	public void traceChange(Trace trace, boolean currentAnimationChanged) {
		try {
			this.trace = trace;
			this.currentValues = this.trace.getCurrentState().getValues();
			this.previousValues = this.trace.canGoBack() ? this.trace.getPreviousState().getValues() : null;
			this.updateElements(this.tvChildrenItem, this.trace.getModel().getChildrenOfType(Machine.class));
		} catch (Exception e) {
			// Otherwise the exception gets lost somewhere deep in a
			// ProB log file, without a traceback.
			e.printStackTrace();
		}
	}

	@Override
	public void animatorStatus(boolean busy) {}
	
	public void showExpression(AbstractFormulaElement formula) {
		FormulaGenerator generator = new FormulaGenerator(animations);
		generator.setFormula(formula.getFormula());
	}

	@FXML
	public void initialize() {
		this.animations.registerAnimationChangeListener(this);

		this.currentValues = null;
		this.previousValues = null;

		this.tvChildrenItem.setValue(new ElementClassStateTreeItem(Machine.class));
		
		tv.setOnMouseClicked(e -> {
			if (tv.getSelectionModel().getSelectedItem() == null) {
				return;
			}
			StateTreeItem<?> selectedItem = tv.getSelectionModel().getSelectedItem().getValue();
			if(selectedItem instanceof ElementStateTreeItem && !((ElementStateTreeItem) selectedItem).getValue().equals("")) {
				showExpression((AbstractFormulaElement)(((ElementStateTreeItem) selectedItem).getContents()));
			}
			tv.getSelectionModel().clearSelection();
		});
		
		this.classBlacklist.getBlacklist().addListener(
			(SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
				if (this.trace != null) {
					this.updateElements(this.tvChildrenItem, this.trace.getModel().getChildrenOfType(Machine.class));
				}
			}
		);
		
		this.classBlacklist.getKnownClasses().add(Action.class);
		this.classBlacklist.getBlacklist().add(Action.class);
	}
}
