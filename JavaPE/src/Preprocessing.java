import java.util.List;

public class Preprocessing {

	public Preprocessing() {
		
	}
	
	public static Input run(Input input){
		Input processedInput = new Input(null,null);
		List<Referee> updatedrefereeList = Common.updateRefereeCapacities(input.getListOfReferees());
		updatedrefereeList = Common.generatePairsFofReferee(updatedrefereeList);
		processedInput.setListOfReferees(updatedrefereeList);
		processedInput.setListOfProposals(input.getListOfProposals());
		List<ProposalPair> proposalPairs = Common.generateProposalPairs(processedInput);
		processedInput.setListOfProposalPairs(proposalPairs);
		return processedInput;
	}

}
