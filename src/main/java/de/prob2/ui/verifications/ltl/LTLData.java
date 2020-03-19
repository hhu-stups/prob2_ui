package de.prob2.ui.verifications.ltl;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.prob2.ui.json.JsonManager;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

public class LTLData {
	public static final JsonDeserializer<LTLData> JSON_DESERIALIZER = LTLData::new;

	private List<LTLFormulaItem> formulas;

	private List<LTLPatternItem> patterns;

	public LTLData(List<LTLFormulaItem> formulas, List<LTLPatternItem> patterns) {
		this.formulas = formulas;
		this.patterns = patterns;
	}

	private LTLData(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.formulas = JsonManager.checkDeserialize(context, object, "formulas", new TypeToken<List<LTLFormulaItem>>() {}.getType());
		this.patterns = JsonManager.checkDeserialize(context, object, "patterns", new TypeToken<List<LTLPatternItem>>() {}.getType());
	}

	public List<LTLFormulaItem> getFormulas() {
		return formulas;
	}

	public List<LTLPatternItem> getPatterns() {
		return patterns;
	}
}
