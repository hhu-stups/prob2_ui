package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TestCaseGenerationItem extends AbstractCheckableItem {
	
	private static class TestCaseGenerationFormulaExtractor {

		private TestCaseGenerationFormulaExtractor(){}

		private static int extractLevel(String formula) {
			String[] splittedStringBySlash = formula.replace(" ", "").split("/");
			String[] splittedStringByColon = splittedStringBySlash[0].split(":");
			return Integer.parseInt(splittedStringByColon[1]);
		}

		private static List<String> extractOperations(String formula) {
			String[] splittedString = formula.replace(" ", "").split("/");
			return Arrays.asList(splittedString[0].split(":")[1].split(","));
		}

	}
	

	private static final String LEVEL = "level";

	private static final String OPERATIONS = "operations";
	
	private int maxDepth;
	
	private transient ListProperty<Trace> examples;
	
	private Map<String, Object> additionalInformation;
	
	private TestCaseGenerationType type;
	

	public TestCaseGenerationItem(String name, TestCaseGenerationType type) {
		super(name, type.getName(), "");
		this.additionalInformation = new HashMap<>();
	}
	
	public TestCaseGenerationItem(String name, TestCaseGenerationType type, Map<String, Object> additionalInformation) {
		super(name, type.getName(), "");
		this.additionalInformation = additionalInformation;
	}
	
	public TestCaseGenerationItem(int maxDepth, int level) {
		super("MCDC:" + level + "/" + "DEPTH:" + maxDepth, TestCaseGenerationType.MCDC.getName(), "");
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.type = TestCaseGenerationType.MCDC;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(LEVEL, level);
	}

	public TestCaseGenerationItem(int maxDepth, List<String> operations) {
		super("OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + maxDepth, TestCaseGenerationType.COVERED_OPERATIONS.getName(), "");
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.type = TestCaseGenerationType.COVERED_OPERATIONS;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(OPERATIONS, operations);
	}
	
	public void replaceMissingWithDefaults() {
		if(this.examples == null) {
			this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		} else {
			this.examples.setValue(FXCollections.observableArrayList());
		}
		if(this.additionalInformation == null) {
			this.additionalInformation = new HashMap<>();
		}
		if(type == TestCaseGenerationType.MCDC) {
			replaceMissingMCDCOptionsByDefaults();
		} else if(type == TestCaseGenerationType.COVERED_OPERATIONS) {
			replaceMissingCoveredOperationsOptionsByDefaults();
		}
	}
	
	private void replaceMissingMCDCOptionsByDefaults() {
		if(additionalInformation.get(LEVEL) == null) {
			int level = TestCaseGenerationFormulaExtractor.extractLevel(this.code);
			additionalInformation.put(LEVEL, level);
		}
	}
	
	private void replaceMissingCoveredOperationsOptionsByDefaults() {
		if(additionalInformation.get(OPERATIONS) == null) {
			List<String> operations = TestCaseGenerationFormulaExtractor.extractOperations(this.code);
			additionalInformation.put(OPERATIONS, operations);
		}
	}
	
	public Object getAdditionalInformation(String key) {
		return additionalInformation.get(key);
	}

	public void putAdditionalInformation(String key, Object value) {
		additionalInformation.put(key, value);
	}
	
	public TestCaseGenerationType getType() {
		return type;
	}

	public int getMaxDepth() {
		return maxDepth;
	}
	
	public ListProperty<Trace> examplesProperty() {
		return examples;
	}
	
	public ObservableList<Trace> getExamples() {
		return examples.get();
	}
	
	public void reset() {
		this.initialize();
	}
	
	public void setData(String name, String description, String code, TestCaseGenerationType type, int maxDepth, Map<String, Object> additionalInformation) {
		super.setData(name, description, code);
		this.type = type;
		this.maxDepth = maxDepth;
		this.additionalInformation = additionalInformation;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TestCaseGenerationItem)) {
			return false;
		}
		TestCaseGenerationItem otherItem = (TestCaseGenerationItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name, code, type);
	}
}
