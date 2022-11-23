package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.List;

@Singleton
public class POManager {

	private final ListProperty<ProofObligationItem> proofObligations;

	@Inject
	public POManager() {
		this.proofObligations = new SimpleListProperty<>(FXCollections.observableArrayList());
	}

	public void setProofObligations(List<ProofObligationItem> proofObligations) {
		this.proofObligations.clear();
		this.proofObligations.addAll(proofObligations);
	}

	public ListProperty<ProofObligationItem> proofObligationsProperty() {
		return proofObligations;
	}

	public List<ProofObligationItem> getProofObligations() {
		return proofObligations.get();
	}
}
