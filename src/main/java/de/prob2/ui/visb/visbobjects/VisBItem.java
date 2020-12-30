package de.prob2.ui.visb.visbobjects;

//import de.prob2.ui.visb.VisBParser;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob2.ui.visb.exceptions.VisBNestedException;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob.animator.domainobjects.FormulaExpand;

import java.util.Objects;

/**
 * The VisBItem is designed for the JSON / VisB file
 */
public class VisBItem {
	private String id;
	private String attribute;
	private String value; // B Formula to compute value of attribute for SVG object id
	private Boolean optional; // if true then we ignore identifier not found errors and simply disable this item
	private IEvalElement parsedFormula; // if different from null the formula has already been parsed

	/**
	 *
	 * @param id this has to be the id used in the svg file to correspond with that svg element
	 * @param attribute this has to be an actual svg attribute, that can be handled via {@link VisBParser}
	 * @param value this formula has to provide a valid value usable with the given attribute
	 * @param optional true if this item is optional, i.e., will be ignored if the value formula contains unknown ids
	 */
	public VisBItem(String id, String attribute, String value, Boolean optional) {
		this.id = id;
		this.attribute = attribute.toLowerCase();
		this.value = value;
		this.optional = optional;
	}

	public String getId() {
		return id;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getValue() {
		return value;
	}

	public Boolean itemIsOptional() {
		return optional;
	}
	public IEvalElement getParsedFormula() {
	 // getter which does not require CurrentTrace; but cannot parse on demand
		return parsedFormula;
	}
	
	/**
	 * parse the formula of VisBItem and store parsed formula in parsedFormula attribute
	 */
	public IEvalElement getParsedValueFormula(CurrentTrace currentTrace) throws VisBNestedException, ProBError {
		String formulaToEval = this.getValue();
		try {
			if (this.parsedFormula != null) {
			   return this.parsedFormula; // is already parsed
			} else if(currentTrace.getModel() instanceof ClassicalBModel) {
			   this.parsedFormula = currentTrace.getModel().parseFormula(formulaToEval, FormulaExpand.EXPAND);
			  // use parser associated with the current model, DEFINITIONS are accessible
			} else {
			   this.parsedFormula = new ClassicalB(formulaToEval, FormulaExpand.EXPAND); // use classicalB parser
			   // Note: Rodin parser does not have IF-THEN-ELSE nor STRING manipulation, cumbersome for VisB
			}
		} catch (EvaluationException e){
			System.out.println("\nException for "+ this.getId() + "."+ this.getAttribute() + " : " + e);
		    throw(new VisBNestedException("Exception parsing B formula for VisB item "+ this.getId() + "."+ this.getAttribute() + " : ",e));
		}
	    return this.parsedFormula;
	}
	
	
	@Override
	public String toString(){
		return "{ID: " + this.id +", ATTRIBUTE: "+this.attribute+", VALUE: "+this.value+"} ";
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof VisBItem)) {
			return false;
		}
		VisBItem other = (VisBItem) obj;
		return this.id.equals(other.id) && this.attribute.equals(other.attribute) && this.value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, attribute, value);
	}
}
