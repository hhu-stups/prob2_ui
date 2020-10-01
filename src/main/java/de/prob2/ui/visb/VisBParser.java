package de.prob2.ui.visb;

import java.util.ArrayList;
import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.WDError;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.visb.exceptions.VisBParseException;
import de.prob2.ui.visb.exceptions.VisBNestedException;
import de.prob2.ui.visb.visbobjects.VisBItem;

import javafx.scene.paint.Color;

/**
 * This class will get the items of an VisB visualisation and evaluate them to construct executable JQueries.
 */
@Singleton
public class VisBParser {
	private final CurrentTrace currentTrace;
	private MachineLoader machineLoader;

	/**
	 * As all other constructors in this plugin, this one needs interaction with the ProB2-UI to be able to evaluate the visualisation items.
	 * @param machineLoader only needed to a valid empty trace
	 * @param currentTrace needed to evaluate the formulas on the current trace
	 */
	@Inject
	public VisBParser(final MachineLoader machineLoader, final CurrentTrace currentTrace){
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;
	}

	/**
	 * Uses evaluateFormula to evaluate the visualisation items.
	 * @param visItems items given by the {@link VisBController}
	 * @return all needed jQueries in one string
	 * @throws VisBParseException If the value contains something, that should not be in the JQuery
	 * @throws EvaluationException from evaluating formula on trace
	 * @throws BCompoundException if the B expression is not correct
	 */
	String evaluateFormulas(ArrayList<VisBItem> visItems) throws VisBParseException, EvaluationException, VisBNestedException, BCompoundException{
		StringBuilder jQueryForChanges = new StringBuilder();
		for(VisBItem visItem : visItems){
		        try {
					AbstractEvalResult abstractEvalResult = evaluateFormula(visItem.getValue());
					// TO DO: should we group the evaluation of all Formulas to avoid Prolog calling overhead?
					// We should also parse the formulas only once
					String value = getValueFromResult(abstractEvalResult, visItem);
					String jQueryTemp = getJQueryFromInput(visItem.getId(), visItem.getAttribute(), value);
					jQueryForChanges.append(jQueryTemp);
				} catch (EvaluationException e){
					System.out.println("\nException for "+ visItem.getId() + "."+ visItem.getAttribute() + " : " + e);
					// TODO: either add text to exception or be able to call something like alert(e, "visb.exception.header", "visb.infobox.visualisation.formula.error ",visItem.getId(),visItem.getAttribute());
					throw(new VisBNestedException("Exception evaluating B formula for "+ visItem.getId() + "."+ visItem.getAttribute() + " : ",e));
				}
		}
		return jQueryForChanges.toString();
	}

	/**
	 * This idea originated from the BInterpreter in the ProB2-UI. In this method a new trace is constructed, on which the formulas, which have to be evaluated can be evaluated on.
	 * This method makes it not possible, to change the current trace. It was not needed, because the formulas were solely made for producing a value for a JQuery.
	 * @param formulaToEval the formula, that should be evaluated
	 * @return an AbstractEvalResult, that will be further evaluated in the getValueFromResult method
	 * @throws EvaluationException If evaluating formula on trace causes an Exception.
	 * @throws ProBError If a formula cannot correctly be evaluated.
	 */
	private AbstractEvalResult evaluateFormula(String formulaToEval) throws EvaluationException, ProBError {
		//SVG Javascript && ValueTranslator(if necessary)
		IEvalElement formula;
		if(currentTrace.getModel() instanceof ClassicalBModel) {
		   formula = currentTrace.getModel().parseFormula(formulaToEval, FormulaExpand.EXPAND);
		  // use parser associated with the current model, DEFINITIONS are accessible
		} else {
		   formula = new ClassicalB(formulaToEval, FormulaExpand.EXPAND); // use classicalB parser
		   // Note: Rodin parser does not have IF-THEN-ELSE nor STRING manipulation, possibly too cumbersome for VisB
		}
		
		//Take the current trace
		Trace trace = currentTrace.get();
		if (trace == null) {
			trace = new Trace(machineLoader.getEmptyStateSpace());
		}
		//Evaluate the formula on the current trace
		return trace.evalCurrent(formula);
	}

	/**
	 * The idea for this method originated from the BInterpreter, as well. It was just modified to fit the purpose of this plugin.
	 * In this method the ValueTranslator is used to translate the AbstractEvalResult, that was returned from the previous method. The result is a String value, which can be further analysed or used directly as an attribute in the JQuery.
	 * @param abstractEvalResult result of the previous method
	 * @return value in string format, that represents the new value for the attribute given in the JSON / VisB file or VisBVisualisation VisBItems list
	 * @throws IllegalArgumentException If the result of the formula cannot be shown properly
	 * @throws VisBParseException If the result is something different, than an EvalResult, which should not be the case, if formula is stated correctly
	 * @throws BCompoundException If a BCompoundException is thrown while translating with the Translator
	 */
	private String getValueFromResult(AbstractEvalResult abstractEvalResult, VisBItem visBItem) throws IllegalArgumentException, VisBParseException, BCompoundException {
		String value;
		Objects.requireNonNull(abstractEvalResult);
		if (abstractEvalResult instanceof EvalResult) {
			value = abstractEvalResult.toString().replaceAll("^\"|\"$", ""); // remove leading and trailing double quotes from B string values
		} else if (abstractEvalResult instanceof EvaluationErrorResult) {
			if (abstractEvalResult instanceof IdentifierNotInitialised) {
				//Identifier not initialised
				throw new VisBParseException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". An identifier has not been initialised."+((EvaluationErrorResult) abstractEvalResult).getErrors());
			} else if (abstractEvalResult instanceof WDError) {
				//Identifier not well defined
				throw new VisBParseException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". A well-definedness error occurred."+((EvaluationErrorResult) abstractEvalResult).getErrors());
			} else {
				//Formula evaluation error
				throw new VisBParseException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". There was an error evaluating the formula."+((EvaluationErrorResult) abstractEvalResult).getErrors());
			}
		} else if (abstractEvalResult instanceof EnumerationWarning) {
			//enumeration warning
			throw new VisBParseException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". There was an enumeration warning while evaluating the formula."+abstractEvalResult.toString());
		} else if (abstractEvalResult instanceof ComputationNotCompletedResult) {
			//computation not completed
			throw new VisBParseException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". Computation of the formula was not completed."+((ComputationNotCompletedResult) abstractEvalResult).getReason());
		} else {
			throw new IllegalArgumentException("There was a problem translating your formula for id: \""+visBItem.getId()+"\", and attribute: \""+visBItem.getAttribute()+"\""+". The result of this formula cannot be shown.");
		}
		return value;
	}

	/**
	 * This is a rather long method, for a rather short purpose. The purpose is to check the string, that is the return value of the previous method for anything, that shouldn't be in the jQuery later on.
	 * @param id field of the VisBItem
	 * @param attr field of the VisBItem
	 * @param value field of the VisBItem
	 * @return returns the final jQuery string with the id, the attribute and the value in it. This string can be executed as JQuery.
	 * @throws VisBParseException If the value contains something, that should not be in the JQuery
	 */
	private String getJQueryFromInput(String id, String attr, String value) throws VisBParseException{
		boolean checkValue = false;
		String length_per = "(0|([1-9]+\\d*))[%]?(em)?(ex)?(px)?(in)?(cm)?(mm)?(pt)?(pc)?";
		String number_per = "^(0|([1-9]+\\d*))[%]?$";
		String verified_string = "^[a-zA-Z][a-zA-Z0-9_\\-]*$";
		switch(attr){
			case "alignment-baseline":
				String[] valueTypesAB = {"auto", "baseline", "before-edge", "text-before-edge", "middle", "central", "after-edge", "text-after-edge", "ideographic", "alphabetic", "hanging", "mathematical", "top", "center", "bottom"};
				for(String s : valueTypesAB) {
					if(s.equals(value)) {
						checkValue = true;
					}
				}
				break;
			case "cclass":
				//Regex matches class names
				checkValue = value.matches(verified_string);
				break;
			case "clipPathUnits":
			case "maskUnits":
				checkValue = "userSpaceOnUse".equals(value) || "objectBoundingBox".equals(value);
				break;
			case "clip-path":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "clip-rule":
				checkValue = "nonezero".equals(value) || "evenodd".equals(value) || "inherit".equals(value);
				break;
			case "cx":
			case "cy":
				//Checks the value for regex for length-percentage
				checkValue = value.matches("^calc\\("+length_per+" [+\\-*/]{1}+ "+length_per+"\\)$") ||
						value.matches("^"+length_per+"$");
				break;
			case "d":
				//TODO: How do I verify that?
				checkValue = true;
				break;
			case "direction":
				checkValue = value.equals("ltr") || value.equals("rtl");
				break;
			case "display":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "dx":
			case "dy":
				//Checking for length - list
				checkValue = value.matches("^(" + length_per + " )*" + length_per + "$");
				break;
			case "fill":
				//Checking for color
				try {
					Color.web(value);
					checkValue = true;
				} catch (IllegalArgumentException e){
					checkValue = false;
				}
				break;
			case "fill-opacity":
				if(value.matches("^"+number_per+"$")){
					checkValue = true;
				} else{
					checkValue = checkNumber(value);
				}
				break;
			case "fill-rule":
				checkValue = "evenodd".equals(value) || "nonzero".equals(value);
				break;
			case "font-family":
				//TODO: This can be done better, I think
				checkValue = value.matches(verified_string);
				break;
			case "font-size":
				checkValue = value.matches(length_per) || "smaller".equals(value) || "bigger".equals(value);
				break;
			case "font-stretch":
				//Chech for percentage and values
				if(value.matches("^"+number_per+"$")){
					checkValue = true;
				}
				String[] valueTypesFS = {"normal", "ultra-condensed", "extra-condensed", "condensed", "semi-condensed", "semi-expanded", "expanded", "extra-expanded", "ultra-expanded"};
				for(String s : valueTypesFS){
					if(s.equals(value)){
						checkValue = true;
					}
				}
				break;
			case "font-style":
				checkValue = "italic".equals(value) || "normal".equals(value) || "oblique".equals(value);
				break;
			case "font-variant":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "font-weight":
				checkValue = "normal".equals(value) || "bold".equals(value) || "bolder".equals(value) || "lighter".equals(value) || value.matches("[0-9]+[0-9]*");
				break;
			case "height":
			case "markerHeight":
			case "markerWidth":
			case "r":
			case "rx":
			case "ry":
			case "stroke-dashoffset":
			case "stroke-width":
			case "textLength":
			case "width":
				checkValue = checkNumber(value);
				if(!checkValue){
					checkValue = value.matches("^"+length_per+"$");
				}
				break;
			case "id":
				checkValue = value.matches("^"+verified_string+"$");
				break;
			case "lengthAdjust":
				checkValue = "spacing".equals(value) || "spacingAndGlyphs".equals(value);
				break;
			case "letter-spacing":
				if("normal".equals(value)){
					checkValue = true;
				} else {
					checkValue = checkNumber(value);
				}
				break;
				//All those are marker references
			case "marker-end":
				checkValue = true;
				break;
			case "marker-mid":
				checkValue = true;
				break;
			case "marker-start":
				checkValue = true;
				break;
			case "markerUnits":
				checkValue = "userSpaceOnUse".equals(value) || "strokeWidth".equals(value);
				break;
			case "maskContentUnits":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "opacity":
			case "stroke-miterlimit":
			case "x":
			case "x1":
			case "x2":
			case "y":
			case "y1":
			case "y2":
				checkValue = checkNumber(value);
				break;
			case "paint-order":
				checkValue = "normal".equals(value) || "fill".equals(value) || "stroke".equals(value) || "markers".equals(value);
				break;
			case "pointer-events":
				String[] valueTypesPE = {"bounding-box", "visiblePainted", "visibleFill", "visibleStroke", "visible", "painted", "fill", "stroke", "all", "none"};
				for(String s : valueTypesPE) {
					if(s.equals(value)) {
						checkValue = true;
					}
				}
				break;
			case "points":
				/*String[] splitted = value.split(" ");
				ArrayList<String> numbers = new ArrayList<>();
				for(String s : splitted){
					String[] num = s.split(",");
					if(num.length == 2) {
						numbers.add(num[0]);
						numbers.add(num[1]);
					}
				}
				for(String s : numbers){
					if(!checkNumber(s)){
						break;
					}
				}*/
				checkValue = true;
				break;
			case "stroke":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "stroke-dasharray":
				checkValue = true;
				break;
			case "stroke-linecap":
				checkValue = "butt".equals(value) || "round".equals(value) || "square".equals(value);
				break;
			case "stroke-linejoin":
				checkValue = "arcs".equals(value) || "bevel".equals(value) || "miter".equals(value) || "miter-clip".equals(value) || "round".equals(value);
				break;
			case "stroke-opacity":
				checkValue = checkNumber(value);
				if(!checkValue){
					checkValue = value.matches("^"+length_per+"$");
				}
				break;
			case "style":
				checkValue = true;
				break;
			case "tabindex":
				try{
					Integer.parseInt(value);
					checkValue = true;
				} catch (NumberFormatException e){
					checkValue = false;
				}
				break;
			case "text-anchor":
				checkValue = "start".equals(value) || "middle".equals(value) || "end".equals(value);
				break;
			case "text-decoration":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "transform":
				//TODO: How do I verify this?
				checkValue = true;
				break;
			case "visibility":
				String[] valueTypesVisibility = {"visible", "hidden", "inherit", "collapse"};
				for(String s : valueTypesVisibility) {
					if(s.equals(value)) {
						checkValue = true;
					}
				}
				break;
			case "word-spacing":
				break;
			case "text":
				return "$(\"#"+id+"\").text(\""+value+"\");\n";
			default:
				throw new VisBParseException("The following attribute cannot be used: \'"+attr+
											 "\'. Check the VisB help section for supported attributes.");
		}
		if(!checkValue) {
			throw new VisBParseException("The value \'" + value + 
			"\' for id \'" + id + 
			"\' cannot be parsed for the attribute \'"+ attr+
			"\'. Check the VisB help section for supported values.");
		}
		return "changeAttribute(\"#"+id+"\", \""+attr+"\", \""+value+"\");\n";
	}

	/**
	 * This checks if a string value is a valid number
	 * @param value string representation of any number
	 * @return true, if it is a valid number, else otherwise
	 */
	private boolean checkNumber(String value){
		try{
			Float.parseFloat(value.replace(",","."));
			return true;
		} catch (NumberFormatException e){
			//Nothing happens here, the exception will be thrown later on
			return false;
		}
	}
}
