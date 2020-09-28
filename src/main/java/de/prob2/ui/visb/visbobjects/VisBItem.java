package de.prob2.ui.visb.visbobjects;

import de.prob2.ui.visb.VisBParser;

/**
 * The VisBItem is designed for the JSON / VisB file
 */
public class VisBItem {
	private String id;
	private String attribute;
	private String value;

	/**
	 *
	 * @param id this has to be the id used in the svg file to correspond with that svg element
	 * @param attribute this has to be an actual svg attribute, that can be handled via {@link VisBParser}
	 * @param value this has to be a valid value usable with the given attribute in string format
	 */
	public VisBItem(String id, String attribute, String value) {
		this.id = id;
		this.attribute = attribute.toLowerCase();
		this.value = value;
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
	@Override
	public String toString(){
		return "{ID: " + this.id +", ATTRIBUTE: "+this.attribute+", VALUE: "+this.value+"} ";
	}
}
