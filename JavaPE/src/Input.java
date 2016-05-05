import java.util.List;

public class Input {

	public Input(List<Referee> listOfReferees, List<Proposal> listOfProposals) {
		this.listOfReferees=listOfReferees;
		this.listOfProposals=listOfProposals;
	}
	
	private List<Referee> listOfReferees;
	private List<Proposal> listOfProposals;
	private List<ProposalPair> listOfProposalPairs;
	
	public List<Referee> getListOfReferees() {
		return listOfReferees;
	}
	public void setListOfReferees(List<Referee> lisOfReferees) {
		this.listOfReferees = lisOfReferees;
	}

	public List<Proposal> getListOfProposals() {
		return listOfProposals;
	}

	public void setListOfProposals(List<Proposal> listOfProposals) {
		this.listOfProposals = listOfProposals;
	}
	public List<ProposalPair> getListOfProposalPairs() {
		return listOfProposalPairs;
	}
	public void setListOfProposalPairs(List<ProposalPair> listOfProposalPairs) {
		this.listOfProposalPairs = listOfProposalPairs;
	}

}
