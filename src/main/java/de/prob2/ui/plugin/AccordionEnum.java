package de.prob2.ui.plugin;

public enum AccordionEnum {
	RIGHT_ACCORDION("rightAccordion"),
	RIGHT_ACCORDION_1("rightAccordion1"),
	RIGHT_ACCORDION_2("rightAccordion2"),
	LEFT_ACCORDION("leftAccordion"),
	LEFT_ACCORDION_1("leftAccordion1"),
	LEFT_ACCORDION_2("leftAccordion2"),
	TOP_ACCORDION("topAccordion"),
	TOP_ACCORDION_1("topAccordion1"),
	TOP_ACCORDION_2("topAccordion2"),
	TOP_ACCORDION_3("topAccordion3"),
	BOTTOM_ACCORDION("bottomAccordion"),
	BOTTOM_ACCORDION_1("bottomAccordion1"),
	BOTTOM_ACCORDION_2("bottomAccordion2"),
	BOTTOM_ACCORDION_3("bottomAccordion3");

	private final String id;

	AccordionEnum(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}
}
