package de.prob2.ui.documentation;

import de.prob2.ui.project.machines.Machine;

//Wrapper for Machine to let User decide Machines to be documented in SaveDocumentationStage
public class MachineDocumentationItem {
	Boolean document;
	final Machine machineItem;

	public MachineDocumentationItem(Boolean document, Machine machineItem) {
		this.document = document;
		this.machineItem = machineItem;
	}

	public Machine getMachineItem() {
		return machineItem;
	}

	public Boolean getDocument() {
		return document;
	}

	public void setDocument(Boolean checked) {
		this.document = checked;
	}
}
