import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ilog.concert.IloException;

public class Main {

	public Main() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		File folder = new File("resources");
		String path = folder.getPath();
		String caseName = "a_01";
		Input input = readFile(path + "/" + caseName + ".txt");
		Input processedInput = Preprocessing.run(input);

		System.out.println("Solving instance " + caseName + ".txt");
		try {
			HeuristicSolution initialSolution = new HeuristicSolution(processedInput, path, caseName, true);
			initialSolution.generateInitialSolution();
			System.out.println("Heuristic sol:" + initialSolution.getObjectiveValue());
			Solver solver = new Solver(processedInput, path, true);
			solver.setBestIntegerValue(initialSolution.getObjectiveValue());
			solver.solve();
		} catch (IloException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println();

	}

	public static Input readFile(String fileName) {

		List<Referee> listOfReferees = new ArrayList<>();
		List<Proposal> listOfProposals = new ArrayList<>();
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(fileName));
			int numOfProposals = 0;
			int refereeID = 0;
			int refCount = 0;
			while (true) {
				String line = br.readLine();
				if (line == null || line.isEmpty())
					break;

				String[] tokens = line.split("\t");
				if (tokens.length > 1) { // capability matrix
					numOfProposals = tokens.length;
					refereeID += 1;
					List<Proposal> capabilityList = new ArrayList<>();
					for (int j = 0; j < tokens.length; j++) {
						if (Integer.parseInt(tokens[j]) == 1) {
							Proposal proposal = new Proposal(j + 1, null);
							capabilityList.add(proposal);
						}
					}
					Referee referee = new Referee(refereeID, 0, capabilityList);
					listOfReferees.add(referee);
				} else {
					listOfReferees.get(refCount).setCapacity(Integer.parseInt(tokens[0]));
					refCount += 1;
				}
			}
			br.close();

			for (int j = 0; j < numOfProposals; j++) {
				List<Referee> capableRefereesList = new ArrayList<>();
				for (Referee referee : listOfReferees) {
					for (Proposal p : referee.getCapabilityList()) {
						if (p.getId() == j + 1) {
							capableRefereesList.add(referee);
							break;
						}
					}
				}
				if (capableRefereesList.size() > 0) {
					Proposal proposal = new Proposal(j + 1, capableRefereesList);
					listOfProposals.add(proposal);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		Input input = new Input(listOfReferees, listOfProposals);
		return input;
	}

}
