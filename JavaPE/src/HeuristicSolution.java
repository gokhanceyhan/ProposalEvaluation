import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeuristicSolution {

	private final boolean debug;
	private String path;
	private String caseName;
	private Input data;
	private Map<String, ProposalPair> mapOfPairs;
	private Map<String, Integer> numberOfPairReviews;
	private Map<String, Integer> numberOfSingleReviewsOfFirstProp;
	private Map<String, Integer> numberOfSingleReviewsOfSecondProp;
	private Map<String, Double> pairWeight;
	private Map<Integer, Integer> refereeCapacity;
	private double objectiveValue;
	private Map<Integer, List<Proposal>> assignments;

	public HeuristicSolution(Input data, String path, String caseName, boolean debug) {
		this.debug = debug;
		this.path = path;
		this.caseName = caseName;
		this.data = data;
		this.mapOfPairs = new HashMap<>();
		this.numberOfPairReviews = new HashMap<>();
		this.numberOfSingleReviewsOfFirstProp = new HashMap<>();
		this.numberOfSingleReviewsOfSecondProp = new HashMap<>();
		this.pairWeight = new HashMap<>();
		this.refereeCapacity = new HashMap<>();
		this.objectiveValue = 0;
		this.assignments = new HashMap<>();
	}

	public void evaluateGivenSolution() {
		createPairMap();
		initializeAssignments();
		BufferedReader br = null;
		try {
			String fileName = path + "/" + caseName + "x.txt";
			br = new BufferedReader(new FileReader(fileName));
			for(Referee referee: data.getListOfReferees()) {
				String line = br.readLine();
				if (line == null || line.isEmpty())
					break;
				String[] tokens = line.split("\t");
				List<Proposal> refereeProposalList = new ArrayList<>();
				for (int j = 0; j < tokens.length; j++) {
					if(Integer.parseInt(tokens[j])==1){
						Proposal proposal = data.getListOfProposals().get(j);
						refereeProposalList.add(proposal);
					}
				}
				assignments.put(referee.getId(), refereeProposalList);
			}
			br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		for (ProposalPair pair : data.getListOfProposalPairs()) {
			int npq = calculateNumberOfPairReviews(pair);
			objectiveValue += Common.calculateWeight(npq);
		}

	}

	public void generateInitialSolution() throws Exception {
		createPairMap();
		initializeAssignments();
		initializeRefereeCapacities();
		while (isThereAvailableReferee()) {
			calculateReviewStats();
			calculateWeights();
			ProposalPair chosenPair = choosePair();
			while (!assignPair(chosenPair)) {
				chosenPair = choosePair();
			}
		}
		for (ProposalPair pair : data.getListOfProposalPairs()) {
			int npq = calculateNumberOfPairReviews(pair);
			objectiveValue += Common.calculateWeight(npq);
		}
	}

	private boolean assignPair(ProposalPair pair) {
		boolean flag = false;
		List<Referee> listOfCandidateReferees = new ArrayList<>();
		for (Referee referee : pair.getReviewers()) {
			boolean reviewP1 = assignments.get(referee.getId()).contains(pair.getP1());
			boolean reviewP2 = assignments.get(referee.getId()).contains(pair.getP2());
			int capacity = refereeCapacity.get(referee.getId());
			if ((!reviewP1) && (!reviewP2) && (capacity >= 2)) {
				// both proposals can be assigned to this referee
				listOfCandidateReferees.add(referee);
			} else if (!(reviewP1) && reviewP2 && (capacity >= 1)) {
				// one of the proposals can be assigned as well
				listOfCandidateReferees.add(referee);
			} else if (reviewP1 && !(reviewP2) && (capacity >= 1)) {
				// one of the proposals can be assigned as well
				listOfCandidateReferees.add(referee);
			}
		}
		if (!listOfCandidateReferees.isEmpty())

		{
			flag = true;
			Referee chosenReferee = new Referee();
			int maxCapacity = 0;
			for (Referee referee : listOfCandidateReferees) {
				if (refereeCapacity.get(referee.getId()) > maxCapacity) {
					maxCapacity = refereeCapacity.get(referee.getId());
					chosenReferee = referee;
				}
			}
			// assign the pair to the chosen referee
			int capacity = refereeCapacity.get(chosenReferee.getId());
			Proposal p1 = pair.getP1();
			Proposal p2 = pair.getP2();
			List<Proposal> proposalList = assignments.get(chosenReferee.getId());
			if (!proposalList.contains(p1)) {
				proposalList.add(p1);
				capacity -= 1;
			}
			if (!proposalList.contains(p2)) {
				proposalList.add(p2);
				capacity -= 1;
			}
			assignments.put(chosenReferee.getId(), proposalList);
			refereeCapacity.put(chosenReferee.getId(), capacity);

		} else

		{
			// discard this pair from future considerations
			mapOfPairs.remove(pair.getId());
			numberOfPairReviews.remove(pair.getId());
			numberOfSingleReviewsOfFirstProp.remove(pair.getId());
			numberOfSingleReviewsOfSecondProp.remove(pair.getId());
			pairWeight.remove(pair.getId());
		}

		return flag;

	}

	private ProposalPair choosePair() throws Exception {
		String chosenPairId = "0_0";
		double minWeight = Double.MAX_VALUE;
		for (Map.Entry<String, Double> pairId : pairWeight.entrySet()) {
			if (pairId.getValue().compareTo(minWeight) <= 0) {
				chosenPairId = pairId.getKey();
				minWeight = pairId.getValue();
			}
		}
		if (chosenPairId.equals("0_0"))
			throw new Exception("Problem in choose pair!");
		return mapOfPairs.get(chosenPairId);
	}

	private void calculateWeights() {
		for (Map.Entry<String, ProposalPair> pair : mapOfPairs.entrySet()) {
			int tpq = pair.getValue().getReviewers().size();
			int npq = numberOfPairReviews.get(pair.getValue().getId());
			int np_q = numberOfSingleReviewsOfFirstProp.get(pair.getValue().getId());
			int nq_p = numberOfSingleReviewsOfSecondProp.get(pair.getValue().getId());
			double weight = tpq * (1 + 2 * npq * (1 + 2 * Math.sqrt(np_q * nq_p)));
			pairWeight.put(pair.getValue().getId(), weight);
		}
	}

	private void initializeRefereeCapacities() {
		for (Referee referee : data.getListOfReferees()) {
			refereeCapacity.put(referee.getId(), referee.getCapacity());
		}
	}

	private void initializeAssignments() {
		for (Referee referee : data.getListOfReferees())
			assignments.put(referee.getId(), new ArrayList<>());
	}

	private void calculateReviewStats() {
		for (ProposalPair pair : data.getListOfProposalPairs()) {
			numberOfPairReviews.put(pair.getId(), calculateNumberOfPairReviews(pair));
			numberOfSingleReviewsOfFirstProp.put(pair.getId(), calculateNumberOfSingleReviewsOfFirstProp(pair));
			numberOfSingleReviewsOfSecondProp.put(pair.getId(), calculateNumberOfSingleReviewsOfSecondProp(pair));
		}
	}

	private int calculateNumberOfPairReviews(ProposalPair pair) {
		int count = 0;
		for (Referee referee : pair.getReviewers()) {
			List<Proposal> refereeReviewList = assignments.get(referee.getId());
			if (!refereeReviewList.isEmpty()) {
				if (refereeReviewList.contains(pair.getP1()) && refereeReviewList.contains(pair.getP2()))
					count += 1;
			}

		}
		return count;
	}

	private int calculateNumberOfSingleReviewsOfFirstProp(ProposalPair pair) {
		int count = 0;
		for (Referee referee : pair.getReviewers()) {
			List<Proposal> refereeReviewList = assignments.get(referee.getId());
			if (!refereeReviewList.isEmpty()) {
				if (refereeReviewList.contains(pair.getP1()) && !refereeReviewList.contains(pair.getP2())
						&& (refereeCapacity.get(referee.getId()) > 0))
					count += 1;
			}

		}
		return count;
	}

	private int calculateNumberOfSingleReviewsOfSecondProp(ProposalPair pair) {
		int count = 0;
		for (Referee referee : pair.getReviewers()) {
			List<Proposal> refereeReviewList = assignments.get(referee.getId());
			if (!refereeReviewList.isEmpty()) {
				if (!refereeReviewList.contains(pair.getP1()) && refereeReviewList.contains(pair.getP2())
						&& (refereeCapacity.get(referee.getId()) > 0))
					count += 1;
			}

		}
		return count;
	}

	private boolean isThereAvailableReferee() {
		boolean flag = false;
		for (Map.Entry<Integer, Integer> referee : refereeCapacity.entrySet()) {
			if (referee.getValue() > 0) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	private void createPairMap() {
		for (ProposalPair pair : data.getListOfProposalPairs())
			mapOfPairs.put(pair.getId(), pair);
	}

	public Input getData() {
		return data;
	}

	public void setData(Input data) {
		this.data = data;
	}

	public Map<String, Integer> getNumberOfPairReviews() {
		return numberOfPairReviews;
	}

	public void setNumberOfPairReviews(Map<String, Integer> numberOfPairReviews) {
		this.numberOfPairReviews = numberOfPairReviews;
	}

	public Map<String, Integer> getNumberOfSingleReviewsOfFirstProp() {
		return numberOfSingleReviewsOfFirstProp;
	}

	public void setNumberOfSingleReviewsOfFirstProp(Map<String, Integer> numberOfSingleReviewsOfFirstProp) {
		this.numberOfSingleReviewsOfFirstProp = numberOfSingleReviewsOfFirstProp;
	}

	public Map<String, Integer> getNumberOfSingleReviewsOfSecondProp() {
		return numberOfSingleReviewsOfSecondProp;
	}

	public void setNumberOfSingleReviewsOfSecondProp(Map<String, Integer> numberOfSingleReviewsOfSecondProp) {
		this.numberOfSingleReviewsOfSecondProp = numberOfSingleReviewsOfSecondProp;
	}

	public Map<String, Double> getPairWeight() {
		return pairWeight;
	}

	public void setPairWeight(Map<String, Double> pairWeight) {
		this.pairWeight = pairWeight;
	}

	public Map<Integer, Integer> getRefereeCapacity() {
		return refereeCapacity;
	}

	public void setRefereeCapacity(Map<Integer, Integer> refereeCapacity) {
		this.refereeCapacity = refereeCapacity;
	}

	public Map<String, ProposalPair> getMapOfPairs() {
		return mapOfPairs;
	}

	public void setMapOfPairs(Map<String, ProposalPair> mapOfPairs) {
		this.mapOfPairs = mapOfPairs;
	}

	public double getObjectiveValue() {
		return objectiveValue;
	}

	public void setObjectiveValue(double objectiveValue) {
		this.objectiveValue = objectiveValue;
	}

	public Map<Integer, List<Proposal>> getAssignments() {
		return assignments;
	}

	public void setAssignments(Map<Integer, List<Proposal>> assignments) {
		this.assignments = assignments;
	}

	public boolean isDebug() {
		return debug;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCaseName() {
		return caseName;
	}

	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}

}
