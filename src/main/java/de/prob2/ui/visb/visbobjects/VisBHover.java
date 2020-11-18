package de.prob2.ui.visb.visbobjects;


/**
 * The VisBEvent is designed for the JSON / VisB file
 */
public class VisBHover {
	private String hoverID; // id of the object whose attribute is modified upon hover
	private String hoverAttribute;
	private String hoverEnterValue;
	private String hoverLeaveValue;

	public VisBHover(String hoverID, String hoverAttribute, String hoverEnterValue, String hoverLeaveValue){
		this.hoverID = hoverID;
		this.hoverAttribute = hoverAttribute;
		this.hoverEnterValue = hoverEnterValue;
		this.hoverLeaveValue = hoverLeaveValue;
	}

	public String getHoverId() {
		return hoverID;
	}
	public String getHoverAttr() {
		return hoverAttribute;
	}
	public String getHoverEnterVal() {
		return hoverEnterValue;
	}
	public String getHoverLeaveVal() {
		return hoverLeaveValue;
	}

	@Override
	public String toString(){
		return "<Hover changing " + hoverID + "." + hoverAttribute+ " upon enter: "+hoverEnterValue+ " leave: " + hoverLeaveValue + ">";
	}
}
