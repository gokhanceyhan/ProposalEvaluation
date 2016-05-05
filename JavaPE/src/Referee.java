import java.util.ArrayList;
import java.util.List;

public class Referee {

	private int id;
	private int capacity;
	private List<Proposal> capabilityList;
	private List<ProposalPair> pairList;

	public Referee() {
		this.id = 0;
		this.capacity = 0;
		this.capabilityList = new ArrayList<>();
		this.pairList = new ArrayList<>();
	}

	public Referee(int id, int capacity, List<Proposal> capabilityList) {
		this.id = id;
		this.capacity = capacity;
		this.capabilityList = capabilityList;
		this.pairList = new ArrayList<>();
	}

	public Referee(Referee that) {
		this.id = that.getId();
		this.capacity = that.getCapacity();
		this.capabilityList = that.getCapabilityList();
		this.pairList = that.getPairList();
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public List<Proposal> getCapabilityList() {
		return capabilityList;
	}

	public void setCapabilityList(List<Proposal> capabilityList) {
		this.capabilityList = capabilityList;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<ProposalPair> getPairList() {
		return pairList;
	}

	public void setPairList(List<ProposalPair> pairList) {
		this.pairList = pairList;
	}
}
