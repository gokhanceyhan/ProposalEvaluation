import java.util.ArrayList;
import java.util.List;

public class Column {
	
	private Referee columnReferee;
	private List<Proposal> columnProposalList;
	private List<ProposalPair> columnPairList;
	private double cost;
	
	public Column() {
		columnReferee = new Referee(0,0,new ArrayList<>());
		columnProposalList = new ArrayList<>();
		columnPairList = new ArrayList<>();
		cost = 0;
	}
	
	public Referee getColumnReferee() {
		return columnReferee;
	}
	public void setColumnReferee(Referee columnReferee) {
		this.columnReferee = columnReferee;
	}

	public List<Proposal> getColumnProposalList() {
		return columnProposalList;
	}

	public void setColumnProposalList(List<Proposal> columnProposalList) {
		this.columnProposalList = columnProposalList;
	}

	public List<ProposalPair> getColumnPairList() {
		return columnPairList;
	}

	public void setColumnPairList(List<ProposalPair> columnPairList) {
		this.columnPairList = columnPairList;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

}
