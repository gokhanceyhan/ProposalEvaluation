import java.util.ArrayList;
import java.util.List;

public class Proposal {

	private Integer id;
	private List<Referee> capableRefereesList;

	public Proposal() {
		id = 0;
		capableRefereesList = new ArrayList<>();
	}

	public Proposal(int id, List<Referee> capableRefereesList) {
		this.id = id;
		this.capableRefereesList = capableRefereesList;
	}

	public Proposal(Proposal that) {
		this.id = that.getId();
		this.capableRefereesList = that.getCapableRefereesList();
	}

	public List<Referee> getCapableRefereesList() {
		return capableRefereesList;
	}

	public void setCapableRefereesList(List<Referee> capableRefereesList) {
		this.capableRefereesList = capableRefereesList;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object v) {
		boolean retVal = false;

		if (v instanceof Proposal) {
			Proposal ptr = (Proposal) v;
			retVal = ptr.id == this.id;
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
