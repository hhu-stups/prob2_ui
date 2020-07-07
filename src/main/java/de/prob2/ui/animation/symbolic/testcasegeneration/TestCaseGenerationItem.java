package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestCaseGenerationItem extends AbstractCheckableItem {
	public static final String LEVEL = "level";
	public static final String OPERATIONS = "operations";
	
	public static final JsonDeserializer<TestCaseGenerationItem> JSON_DESERIALIZER = TestCaseGenerationItem::new;
	
	private int maxDepth;
	
	private final transient ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());
	private final transient ObservableList<TraceInformationItem> traceInformation = FXCollections.observableArrayList();
	private final transient ObservableList<TraceInformationItem> uncoveredOperations = FXCollections.observableArrayList();
	
	private Map<String, Object> additionalInformation;
	
	private TestCaseGenerationType type;
	

	public TestCaseGenerationItem(String name, TestCaseGenerationType type) {
		super(name, type.getName(), "");
		this.type = type;
		this.additionalInformation = new HashMap<>();
	}
	
	public TestCaseGenerationItem(String name, TestCaseGenerationType type, Map<String, Object> additionalInformation) {
		super(name, type.getName(), "");
		this.type = type;
		this.additionalInformation = additionalInformation;
	}
	
	public TestCaseGenerationItem(int maxDepth, int level) {
		super("MCDC:" + level + "/" + "DEPTH:" + maxDepth, TestCaseGenerationType.MCDC.getName(), "");
		this.type = TestCaseGenerationType.MCDC;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(LEVEL, level);
	}

	public TestCaseGenerationItem(int maxDepth, List<String> operations) {
		super("OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + maxDepth, TestCaseGenerationType.COVERED_OPERATIONS.getName(), "");
		this.type = TestCaseGenerationType.COVERED_OPERATIONS;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(OPERATIONS, operations);
	}
	
	private TestCaseGenerationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		final JsonObject object = json.getAsJsonObject();
		this.maxDepth = JsonManager.checkDeserialize(context, object, "maxDepth", int.class);
		this.additionalInformation = JsonManager.checkDeserialize(context, object, "additionalInformation", new TypeToken<Map<String, Object>>() {}.getType());
		this.type = JsonManager.checkDeserialize(context, object, "type", TestCaseGenerationType.class);
	}
	
	@Override
	public void reset() {
		super.reset();
		this.examples.clear();
		this.getTraceInformation().clear();
		this.getUncoveredOperations().clear();
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
	
	public ObservableList<TraceInformationItem> getTraceInformation() {
		return this.traceInformation;
	}
	
	public ObservableList<TraceInformationItem> getUncoveredOperations() {
		return this.uncoveredOperations;
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
		return Objects.hash(this.getName(), this.getCode(), type);
	}
}
