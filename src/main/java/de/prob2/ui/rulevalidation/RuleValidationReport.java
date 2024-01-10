package de.prob2.ui.rulevalidation;

import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import de.prob.model.brules.RuleResults;
import de.prob.model.brules.RuleStatus;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentTrace;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class RuleValidationReport {

	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final VersionInfo versionInfo;
	private final RulesDataModel rulesDataModel;

	public RuleValidationReport(final CurrentTrace currentTrace, final I18n i18n, final VersionInfo versionInfo,
								final RulesDataModel rulesDataModel){
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.versionInfo = versionInfo;
		this.rulesDataModel = rulesDataModel;
	}

	public void reportVelocity(Path path) throws IOException {
		initVelocityEngine();
		VelocityContext context = getVelocityContext();
		try (final Writer writer = Files.newBufferedWriter(path)) {
			Velocity.mergeTemplate("de/prob2/ui/rulevalidation/validation_report.html.vm",
				String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	private static void initVelocityEngine() {
		Properties p = new Properties();
		p.setProperty("resource.loaders", "class");
		p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
	}

	private VelocityContext getVelocityContext() {
		RuleResults ruleResults = new RuleResults(new HashSet<>(rulesDataModel.getRuleMap().values()),
			currentTrace.getCurrentState(), -1);
		RuleResults.ResultSummary resultSummary = ruleResults.getSummary();

		Map<String, List<RuleResult>> sortedClassificationRuleResults = ruleResults.getRuleResultsForClassifications();
		for (List<RuleResult> ruleResult : sortedClassificationRuleResults.values()) {
			ruleResult.sort(Comparator.comparing(RuleResult::getRuleName));
		}

		List<RuleResult> sortedRuleResults = ruleResults.getRuleResultsWithoutClassification();
		sortedRuleResults.sort(Comparator.comparing(RuleResult::getRuleName));

		VelocityContext context = new VelocityContext();
		context.put("i18n", i18n);
		context.put("machineName", currentTrace.getModel().getModelFile().getName());
		context.put("numberOfRules", resultSummary.numberOfRules);
		context.put("rulesChecked", resultSummary.numberOfRules - resultSummary.numberOfRulesNotChecked);
		context.put("rulesSucceeded", resultSummary.numberOfRulesSucceeded);
		context.put("rulesFailed", resultSummary.numberOfRulesFailed);
		context.put("rulesDisabled", resultSummary.numberOfRulesDisabled);
		context.put("classificationMap", sortedClassificationRuleResults);
		context.put("Collectors", Collectors.class);
		context.put("noClassification", sortedRuleResults);
		context.put("status_SUCCESS", RuleStatus.SUCCESS);
		context.put("status_FAIL", RuleStatus.FAIL);
		context.put("status_NOT_CHECKED", RuleStatus.NOT_CHECKED);
		context.put("status_DISABLED", RuleStatus.DISABLED);
		context.put("probCliVersion", versionInfo.getCliVersion().toString());
		context.put("localDateTime", LocalDateTime.now().withNano(0));
		return context;
	}
}
