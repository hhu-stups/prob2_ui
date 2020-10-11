package de.prob2.ui.visb.visbobjects;


/**
 * The VisBEvent is designed for the JSON / VisB file
 */
public class VisBHover {
	private String hover_id; // id of the object whose attribute is modified upon hover
	private String hover_attribute;
	private String hover_enter_value;
	private String hover_leave_value;

	public VisBHover(String hoverid, String attr, String enter, String leave){
		this.hover_id = hoverid;
		this.hover_attribute = attr;
		this.hover_enter_value = enter;
		this.hover_leave_value = leave;
	}

	public String getHoverId() {
		return hover_id;
	}
	public String getHoverAttr() {
		return hover_attribute;
	}
	public String getHoverEnterVal() {
		return hover_enter_value;
	}
	public String getHoverLeaveVal() {
		return hover_leave_value;
	}

	@Override
	public String toString(){
		return "<Hover changing " + hover_id + "." + hover_attribute+ " upon enter: "+hover_enter_value+ " leave: " + hover_leave_value + ">";
	}
}
