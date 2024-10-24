package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.AbstractEditFormulaStage;

public final class EditDynamicFormulaStage extends AbstractEditFormulaStage<VisualizationFormulaTask> {

	private String commandType;

	@Inject
	public EditDynamicFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		super(stageManager, i18n, currentProject, currentTrace);
	}

	public void setCommandType(String commandType) {
		this.commandType = commandType;
	}

	public void setInitialFormulaTask(VisualizationFormulaTask item) {
		super.setInitialFormulaTask(item);
		this.commandType = item.getCommandType();
	}

	@Override
	protected VisualizationFormulaTask createFormulaTask(String id, String formula) {
		return new VisualizationFormulaTask(id, this.commandType, formula);
	}
}
