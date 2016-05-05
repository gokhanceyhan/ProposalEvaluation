import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Common {

	public Common() {
		// TODO Auto-generated constructor stub
	}

	public static List<Referee> updateRefereeCapacities(List<Referee> refereeList) {
		List<Referee> updatedRefereeList = new ArrayList<>();
		for (Referee referee : refereeList) {
			Referee ref = new Referee(referee.getId(), referee.getCapacity(), referee.getCapabilityList());
			if (ref.getCapabilityList().size() < ref.getCapacity())
				ref.setCapacity(referee.getCapabilityList().size());
			if (ref.getCapacity() > 1)
				updatedRefereeList.add(ref);

		}
		return updatedRefereeList;
	}

	public static List<ProposalPair> generateProposalPairs(Input input) {
		List<ProposalPair> proposalPairs = new ArrayList<>();
		for (Proposal p1 : input.getListOfProposals()) {
			for (Proposal p2 : input.getListOfProposals()) {
				if (!(p1.getId() == p2.getId()) && p1.getId() < p2.getId()) {
					ProposalPair proposalPair = new ProposalPair(p1, p2, p1.getId() + "_" + p2.getId());
					List<Referee> refereeList = new ArrayList<>();
					for (Referee referee : input.getListOfReferees()) {
						if (referee.getCapabilityList().contains(p1) && referee.getCapabilityList().contains(p2)) {
							refereeList.add(referee);
						}
					}
					proposalPair.setReviewers(refereeList);
					proposalPairs.add(proposalPair);
				}
			}
		}
		return proposalPairs;
	}

	public static List<Referee> generatePairsFofReferee(List<Referee> referees) {

		for (Referee ref : referees) {
			List<ProposalPair> proposalPairs = new ArrayList<>();
			for (Proposal p1 : ref.getCapabilityList()) {
				for (Proposal p2 : ref.getCapabilityList()) {
					if (!(p1.getId() == p2.getId()) && p1.getId() < p2.getId()) {
						ProposalPair proposalPair = new ProposalPair(p1, p2, p1.getId() + "_" + p2.getId());
						proposalPairs.add(proposalPair);
					}
				}
			}
			ref.setPairList(proposalPairs);
		}
		return referees;
	}

	public static List<ProposalPair> generatePairsForColumn(List<Proposal> proposals) {
		List<ProposalPair> proposalPairs = new ArrayList<>();
		for (Proposal p1 : proposals) {
			for (Proposal p2 : proposals) {
				if (!(p1.getId() == p2.getId()) && p1.getId() < p2.getId()) {
					ProposalPair proposalPair = new ProposalPair(p1, p2, p1.getId() + "_" + p2.getId());
					proposalPairs.add(proposalPair);
				}
			}
		}
		return proposalPairs;

	}

	public static double calculateWeight(int review) {
		double weight = 0.0;
		for (int i = 1; i <= review; i++)
			weight += 1.0 / ((double) i);
		return weight;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static int calculateNumberOfTVariables(List<ProposalPair> pairs) {
		int num = 0;
		for (ProposalPair pair : pairs) {
			num += pair.getReviewers().size();
		}
		return num;
	}

	public static double[] createCoverCoefficientsForXVars(Column column, List<ProposalPair> allPairs) {
		double[] coeffs = new double[allPairs.size()];
		for (int i = 0; i < allPairs.size(); i++) {
			if (column.getColumnPairList().contains(allPairs.get(i))) {
				coeffs[i] = 1;
			} else
				coeffs[i] = 0;
		}
		return coeffs;
	}

	public static double[] createAssignmentCoefficientsForXVars(Column column, List<Referee> allReferees) {
		double[] coeffs = new double[allReferees.size()];
		for (int i = 0; i < allReferees.size(); i++) {
			if (column.getColumnReferee().getId() == allReferees.get(i).getId()) {
				coeffs[i] = 1;
			} else
				coeffs[i] = 0;
		}
		return coeffs;
	}

	public static double[] createWeightCoefficientsForXVars(List<ProposalPair> allPairs) {
		int numOfTVariables = calculateNumberOfTVariables(allPairs);
		double[] coeffs = new double[numOfTVariables];
		for (int i = 0; i < numOfTVariables; i++) {
			coeffs[i] = 0;
		}
		return coeffs;
	}

	public static long getCpuTime() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
	}

	public static boolean isPricingModelSame(Map<String, Double> coverPrices, PricingModel pricingModel) {
		Map<String, Double> oldWeights = pricingModel.getObjWeight();
		boolean flag = true;
		for (Map.Entry<String, Double> pair : oldWeights.entrySet()) {
			Double oldW = pair.getValue();
			Double newW = coverPrices.get(pair.getKey());
			if ((oldW - newW > Constants.equalDoubleTol) || (oldW - newW < -Constants.equalDoubleTol)) {
				flag = false;
				break;
			}
		}
		return flag;
	}

}
