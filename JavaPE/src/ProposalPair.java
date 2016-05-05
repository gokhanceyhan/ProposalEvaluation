import java.util.ArrayList;
import java.util.List;

public class ProposalPair {

	private String id;
	private Proposal p1;
	private Proposal p2;
	private List<Referee> reviewers;
	
	public ProposalPair(){
		id="";
		p1=new Proposal();
		p2=new Proposal();
		reviewers = new ArrayList<>();
	}
	
	public ProposalPair(Proposal p1, Proposal p2, String id) {
		this.setP1(new Proposal(p1));
		this.setP2(new Proposal(p2));
		this.id=id;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Proposal getP1() {
		return p1;
	}
	public void setP1(Proposal p1) {
		this.p1 = p1;
	}
	public Proposal getP2() {
		return p2;
	}
	public void setP2(Proposal p2) {
		this.p2 = p2;
	}
	public List<Referee> getReviewers() {
		return reviewers;
	}
	public void setReviewers(List<Referee> reviewers) {
		this.reviewers = reviewers;
	}
	@Override
	public boolean equals(Object v) {
		boolean retVal = false;

		if (v instanceof ProposalPair) {
			ProposalPair ptr = (ProposalPair) v;
			retVal = (ptr.id.equals(this.id));
		}
		return retVal;
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + (this.id != null ? this.id.hashCode() : 0);
		return hash;
	}
}
