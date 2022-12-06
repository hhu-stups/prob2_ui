package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class POManager {

	private final CurrentProject currentProject;

	@Inject
	public POManager(CurrentProject currentProject) {
		this.currentProject =currentProject;
	}

	public void setProofObligations(List<ProofObligationItem> proofObligations) {
		Machine machine = currentProject.getCurrentMachine();
		List<ProofObligationItem> machinePOs = machine.getProofObligationItems();

		// Store PO Names
		List<String> poNames = proofObligations.stream()
				.map(ProofObligationItem::getName)
				.collect(Collectors.toList());

		List<String> machinePONames = machinePOs.stream()
				.map(ProofObligationItem::getName)
				.collect(Collectors.toList());

		// Add new POs
		proofObligations.stream()
				.filter(po -> !machinePONames.contains(po.getName()))
				.forEach(machinePOs::add);

		// Remove POs that are removed
		List<ProofObligationItem> removedPOs = machinePOs.stream()
				.filter(po -> !poNames.contains(po.getName()))
				.collect(Collectors.toList());
		machinePOs.removeAll(removedPOs);

		// Set discharged status
		Map<String, Boolean> poStatuses = new HashMap<>();
		for(ProofObligationItem po : proofObligations) {
			poStatuses.put(po.getName(), po.isDischarged());
		}

		machinePOs.forEach(po -> po.setDischarged(poStatuses.get(po.getName())));

	}

}
