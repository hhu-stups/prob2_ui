package de.prob2.ui.chart;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.AbstractEditFormulaStage;

public final class EditChartFormulaStage extends AbstractEditFormulaStage<ChartFormulaTask> {

	@Inject
	public EditChartFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, CurrentTrace currentTrace) {
		super(stageManager, i18n, currentProject, currentTrace);
	}

	@Override
	protected ChartFormulaTask createFormulaTask(String id, String formula) {
		return new ChartFormulaTask(id, formula);
	}
}
