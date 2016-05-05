import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;

public class Solver {

	private final boolean debug;
	private Input data;
	private List<Column> listOfColumns;
	private MasterModel masterModel;
	private Map<Integer, PricingModel> pricingModels;
	private String path;
	private double bestIntegerValue;
	private List<Output> openNodes;

	public Solver(Input data, String path, boolean debug) {
		this.debug = debug;
		this.data = data;
		this.path = path;
		this.bestIntegerValue = 0.0;
		this.listOfColumns = new ArrayList<>();
		this.masterModel = new MasterModel();
		this.pricingModels = new HashMap<>();
		this.openNodes = new ArrayList<>();
	}

	public void solve() throws IloException {
		long startCPUTimeNano = Common.getCpuTime();
		generateType2InitialColumns();
		createMasterProblem();
		Map<String, Double> coverPrice = new HashMap<>();
		for (ProposalPair pair : data.getListOfProposalPairs()) {
			coverPrice.put(pair.getId(), 0.0);
		}
		createPricingProblem(coverPrice);
		Output rootNodeSolution = solveMasterProblem(0, 1);
		writeSolution(rootNodeSolution);

		if (rootNodeSolution.getStatus().equals("IntFeasible")) {
			updateBestIntegerValue(rootNodeSolution);
		} else if (rootNodeSolution.getStatus().equals("Feasible")
				&& (rootNodeSolution.getObjectiveValue() > bestIntegerValue + Constants.optimalityTolerance)) {
			if (rootNodeSolution.gettVarStatus().equals("Integer")) {
				openNodes.add(rootNodeSolution);
			} else {
				String branchVar = selectBranchingVariable(rootNodeSolution);
				branch(0, 1, branchVar);
			}
		}

		long taskCPUTimeNano = Common.getCpuTime() - startCPUTimeNano;
		System.out.println("Solution time: " + (double) taskCPUTimeNano / 10e9);
		System.out.println("Best Integer Solution: " + bestIntegerValue);
		System.out.println("Number of open nodes: " + openNodes.size());
		System.out.println("\nComplete.");

	}

	private void branch(int level, int nodeNumber, String branchVar) throws IloException {
		IloNumVar var = masterModel.gettMap().get(branchVar);
		int branchLevel = level + 1;
		// create left child
		int leftNodeNumber = nodeNumber + 1;
		var.setUB(0);
		if (debug)
			masterModel.getMasterProblem().exportModel(path + "/leftChild.lp");
		Output leftSolution = solveMasterProblem(branchLevel, leftNodeNumber);
		writeSolution(leftSolution);
		if (leftSolution.getStatus().equals("IntFeasible")) {
			updateBestIntegerValue(leftSolution);
		} else if (leftSolution.getStatus().equals("Feasible")
				&& (leftSolution.getObjectiveValue() > bestIntegerValue + Constants.optimalityTolerance)) {
			if (leftSolution.gettVarStatus().equals("Integer")) {
				openNodes.add(leftSolution);
			} else {
				String leftBranchVar = selectBranchingVariable(leftSolution);
				branch(branchLevel, leftNodeNumber, leftBranchVar);
			}
		}

		// create right child
		int rightNodeNumber = nodeNumber + 2;
		var.setUB(1);
		var.setLB(1);
		if (debug)
			masterModel.getMasterProblem().exportModel(path + "/rightChild.lp");
		Output rightSolution = solveMasterProblem(branchLevel, rightNodeNumber);
		writeSolution(rightSolution);
		if (rightSolution.getStatus().equals("IntFeasible")) {
			updateBestIntegerValue(rightSolution);
		} else if (rightSolution.getStatus().equals("Feasible")
				&& (rightSolution.getObjectiveValue() > bestIntegerValue + Constants.optimalityTolerance)) {
			if (rightSolution.gettVarStatus().equals("Integer")) {
				openNodes.add(rightSolution);
			} else {
				String rightBranchVar = selectBranchingVariable(rightSolution);
				branch(branchLevel, rightNodeNumber, rightBranchVar);
			}
		}

	}

	private void updateBestIntegerValue(Output solution) {
		if (solution.getObjectiveValue() > bestIntegerValue) {
			bestIntegerValue = solution.getObjectiveValue();
			System.out.println("***Best Integer Value is updated: " + bestIntegerValue);
		}
	}

	private void writeSolution(Output solution) {
		if (debug) {
			System.out.print("Level: " + solution.getLevel() + " Node: " + solution.getNodeNumber() + " Status: "
					+ solution.getStatus() + " Objective: " + Common.round(solution.getObjectiveValue(), 2));
			System.out.print(" Number of columns: " + listOfColumns.size());
			System.out.println(" Fractional column ratio: ");
			int count = 0;
			for (String columnId : solution.getColumnValues().keySet()) {
				if (solution.getColumnValues().get(columnId) < 1 - Constants.intSolTolerance
						&& solution.getColumnValues().get(columnId) > Constants.intSolTolerance) {
					count += 1;
				}
			}
			System.out.println(Common.round((double) count / listOfColumns.size(), 4));
			System.out.println(" # of fract tVar: ");
			count = 0;
			for (String tId : solution.gettValues().keySet()) {
				if (solution.gettValues().get(tId) < 1 - Constants.intSolTolerance
						&& solution.gettValues().get(tId) > Constants.intSolTolerance) {
					count += 1;
				}
			}
			System.out.println(count);
		}
	}

	private String selectBranchingVariable(Output parentNode) {
		String tName = "";
		double frac = 0.5;
		for (Map.Entry<String, Double> tVar : parentNode.gettValues().entrySet()) {
			if (Math.abs(tVar.getValue() - 0.5) < frac) {
				frac = Math.abs(tVar.getValue() - 0.5);
				tName = tVar.getKey();
			}
		}
		return tName;
	}

	private Column solvePricingProblem(Referee referee, Map<String, Double> coverPrices) {
		Column column = new Column();
		column.setColumnReferee(referee);
		PricingModel pricingModel = pricingModels.get(referee.getId());
		try {
			IloCplex pricingProblem = pricingModel.getPricingProblem();
			IloObjective pricingObjective = pricingProblem.getObjective();
			IloLinearNumExpr objfn = pricingProblem.linearNumExpr();
			for (ProposalPair pair : referee.getPairList()) {
				objfn.addTerm(coverPrices.get(pair.getId()), pricingModel.getaMap().get(pair.getId()));
			}
			pricingObjective.setExpr(objfn);
			if (debug)
				pricingProblem.exportModel(path + "/pricingProblem.lp");
			pricingProblem.solve();
			column.setCost(pricingProblem.getObjValue());
			double[] xValues = new double[referee.getCapabilityList().size()];
			xValues = pricingProblem.getValues(pricingModel.getxVar());
			List<Proposal> assignedProposals = new ArrayList<>();
			for (int i = 0; i < referee.getCapabilityList().size(); i++) {
				if (xValues[i] == 1)
					assignedProposals.add(referee.getCapabilityList().get(i));
			}
			column.setColumnProposalList(assignedProposals);
			column.setColumnPairList(Common.generatePairsForColumn(assignedProposals));

		} catch (IloException exc) {
			System.err.println("Concert exception '" + exc + "' caught");
		}
		return column;
	}

	private List<Column> generateColumns(Map<String, Double> coverPrices, Map<Integer, Double> assignmentPriceMap) {
		List<Column> columns = new ArrayList<>();
		for (Referee referee : data.getListOfReferees()) {
			if (!Common.isPricingModelSame(coverPrices, pricingModels.get(referee.getId()))) {
				Column column = solvePricingProblem(referee, coverPrices);
				if (column.getCost() + assignmentPriceMap.get(referee.getId()) < -Constants.reducedCostTolerance)
					columns.add(column);
			}
		}
		return columns;
	}

	private Output solveMasterProblem(int level, int nodeNumber) {
		Output solution = new Output(level, nodeNumber);
		boolean flag = true;
		try {

			IloCplex masterProblem = null;
			while (flag) {
				masterProblem = masterModel.getMasterProblem();
				masterProblem.solve();
				if (debug)
					masterProblem.writeSolution(path + "/masterOutput.txt");
				if (masterProblem.getCplexStatus().equals(CplexStatus.Infeasible))
					break;
				double[] coverPrices = masterProblem.getDuals(masterModel.getCover());
				Map<String, Double> coverPriceMap = new HashMap<>();
				for (int i = 0; i < data.getListOfProposalPairs().size(); i++) {
					coverPriceMap.put(data.getListOfProposalPairs().get(i).getId(), coverPrices[i]);
				}
				double[] assignmentPrices = masterProblem.getDuals(masterModel.getAssignment());
				Map<Integer, Double> assignmentPriceMap = new HashMap<>();
				for (int i = 0; i < data.getListOfReferees().size(); i++) {
					assignmentPriceMap.put(data.getListOfReferees().get(i).getId(), assignmentPrices[i]);
				}
				List<Column> newColumns = generateColumns(coverPriceMap, assignmentPriceMap);
				if (newColumns.size() > 0) {
					masterModel.addColumnsToMasterProblem(data, newColumns, listOfColumns.size());
					if (debug)
						masterProblem.exportModel(path + "/masterProblem.lp");
					listOfColumns.addAll(newColumns);
				} else
					flag = false;
			}
			solution.fillOutputValues(masterModel);

		} catch (IloException exc) {
			System.err.println("Concert exception '" + exc + "' caught");
		}

		return solution;
	}

	private void createPricingProblem(Map<String, Double> coverPrice) {
		try {
			for (Referee referee : data.getListOfReferees()) {
				PricingModel pricingModel = new PricingModel();
				IloCplex pricingProblem = new IloCplex();

				// set parameters
				pricingProblem.setParam(IloCplex.Param.MIP.Display, SolverParameters.pricingDisplay);

				// create decision variables
				IloIntVar[] a = new IloIntVar[referee.getPairList().size()];
				String[] aNames = new String[referee.getPairList().size()];
				for (int i = 0; i < referee.getPairList().size(); i++) {
					aNames[i] = "a_" + referee.getPairList().get(i).getId();
				}
				a = pricingProblem.intVarArray(referee.getPairList().size(), 0, 1, aNames);
				Map<String, IloIntVar> aMap = new HashMap<>();
				for (int i = 0; i < referee.getPairList().size(); i++) {
					aMap.put(referee.getPairList().get(i).getId(), a[i]);
				}

				IloIntVar[] x = new IloIntVar[referee.getCapabilityList().size()];
				String[] xNames = new String[referee.getCapabilityList().size()];
				for (int i = 0; i < referee.getCapabilityList().size(); i++) {
					xNames[i] = "x_" + referee.getCapabilityList().get(i).getId();
				}
				x = pricingProblem.intVarArray(referee.getCapabilityList().size(), 0, 1, xNames);
				Map<Integer, IloIntVar> xMap = new HashMap<>();
				for (int i = 0; i < referee.getCapabilityList().size(); i++) {
					xMap.put(referee.getCapabilityList().get(i).getId(), x[i]);
				}

				// create objective function
				Map<String, Double> objWeight = new HashMap<>();
				IloLinearNumExpr objfn = pricingProblem.linearNumExpr();
				for (ProposalPair pair : referee.getPairList()) {
					objfn.addTerm(coverPrice.get(pair.getId()), aMap.get(pair.getId()));
					// store the coefficients
					objWeight.put(pair.getId(), coverPrice.get(pair.getId()));
				}
				IloObjective pricingObjective = pricingProblem.addMinimize();
				pricingObjective.setExpr(objfn);

				// create constraints
				for (ProposalPair pair : referee.getPairList()) {
					IloLinearNumExpr Eq1 = pricingProblem.linearNumExpr();
					String name1 = "c_" + pair.getId() + "_1";
					Eq1.addTerm(1, aMap.get(pair.getId()));
					Eq1.addTerm(-1, xMap.get(pair.getP1().getId()));
					pricingProblem.addLe(Eq1, 0.0, name1);

					IloLinearNumExpr Eq2 = pricingProblem.linearNumExpr();
					String name2 = "c_" + pair.getId() + "_2";
					Eq2.addTerm(1, aMap.get(pair.getId()));
					Eq2.addTerm(-1, xMap.get(pair.getP2().getId()));
					pricingProblem.addLe(Eq2, 0.0, name2);
				}
				IloLinearNumExpr capacity = pricingProblem.linearNumExpr();
				String name = "cap";
				for (Proposal proposal : referee.getCapabilityList()) {
					capacity.addTerm(1, xMap.get(proposal.getId()));
				}
				pricingProblem.addEq(capacity, referee.getCapacity(), name);
				if (debug)
					pricingProblem.exportModel(path + "/pricingProblem.lp");
				pricingModel.setaVar(a);
				pricingModel.setxVar(x);
				pricingModel.setPricingProblem(pricingProblem);
				pricingModel.setaMap(aMap);
				pricingModel.setObjWeight(objWeight);

				// add the pricing problem of the referee to the list
				pricingModels.put(referee.getId(), pricingModel);
			}

		} catch (IloException exc) {
			System.err.println("Concert exception '" + exc + "' caught");
		}
	}

	private void createMasterProblem() {
		try {
			IloCplex masterProblem = new IloCplex();
			masterProblem.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
			masterProblem.setParam(IloCplex.Param.Preprocessing.Presolve, SolverParameters.paramPreprocess);
			masterProblem.setParam(IloCplex.Param.Simplex.Tolerances.Feasibility,
					SolverParameters.feasibilityTolerance);
			masterProblem.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, SolverParameters.integralityTolerance);
			masterProblem.setParam(IloCplex.Param.Simplex.Display, SolverParameters.masterDisplay);
			// create range arrays

			IloObjective masterObjective = masterProblem.addMaximize();
			IloRange[] cover = new IloRange[data.getListOfProposalPairs().size()];
			for (int c = 0; c < data.getListOfProposalPairs().size(); c++) {
				cover[c] = masterProblem.addRange(0.0,
						(double) data.getListOfProposalPairs().get(c).getReviewers().size());
			}
			IloRange[] assignment = new IloRange[data.getListOfReferees().size()];
			for (int a = 0; a < data.getListOfReferees().size(); a++) {
				assignment[a] = masterProblem.addRange(0.0, 1.0);
			}
			IloRange[] weight = new IloRange[data.getListOfProposalPairs().size()];
			for (int w = 0; w < data.getListOfProposalPairs().size(); w++) {
				weight[w] = masterProblem.addRange(0.0, 1.0);
			}

			// create x variables
			IloNumVar[] x = new IloNumVar[listOfColumns.size()];
			Map<String, IloNumVar> xMap = new HashMap<>();
			for (int i = 0; i < listOfColumns.size(); i++) {
				String xName = "x_" + (i + 1);
				double[] coverCoeffs = Common.createCoverCoefficientsForXVars(listOfColumns.get(i),
						data.getListOfProposalPairs());
				double[] assignmentCoeffs = Common.createAssignmentCoefficientsForXVars(listOfColumns.get(i),
						data.getListOfReferees());
				IloColumn xColumn = masterProblem.column(masterObjective, 0.0);
				for (int c = 0; c < data.getListOfProposalPairs().size(); c++) {
					xColumn = xColumn.and(masterProblem.column(cover[c], coverCoeffs[c]));
				}
				for (int a = 0; a < data.getListOfReferees().size(); a++) {
					xColumn = xColumn.and(masterProblem.column(assignment[a], assignmentCoeffs[a]));
				}
				for (int w = 0; w < data.getListOfProposalPairs().size(); w++) {
					xColumn = xColumn.and(masterProblem.column(weight[w], 0.0));
				}
				x[i] = masterProblem.numVar(xColumn, 0.0, 1.0, xName);
				xMap.put(xName, x[i]);
			}

			// create t variables
			int numOfTVariables = Common.calculateNumberOfTVariables(data.getListOfProposalPairs());
			Map<String, IloNumVar> tMap = new HashMap<>();
			IloNumVar[] t = new IloNumVar[numOfTVariables];
			int count = 0;
			for (ProposalPair pair : data.getListOfProposalPairs()) {
				for (int i = 0; i < pair.getReviewers().size(); i++) {
					String tName = "t_" + pair.getId() + "_" + (i + 1);

					IloColumn tColumn = masterProblem.column(masterObjective, Common.calculateWeight(i + 1));
					for (int c = 0; c < data.getListOfProposalPairs().size(); c++) {
						if (data.getListOfProposalPairs().get(c).equals(pair)) {
							tColumn = tColumn.and(masterProblem.column(cover[c], -(i + 1)));
						} else
							tColumn = tColumn.and(masterProblem.column(cover[c], 0.0));
					}
					for (int a = 0; a < data.getListOfReferees().size(); a++) {
						tColumn = tColumn.and(masterProblem.column(assignment[a], 0.0));
					}
					for (int w = 0; w < data.getListOfProposalPairs().size(); w++) {
						if (data.getListOfProposalPairs().get(w).equals(pair)) {
							tColumn = tColumn.and(masterProblem.column(weight[w], 1.0));
						} else
							tColumn = tColumn.and(masterProblem.column(weight[w], 0.0));
					}
					t[count] = masterProblem.numVar(tColumn, 0.0, 1.0, tName);
					tMap.put(tName, t[count]);
					count += 1;
				}
			}
			if (debug)
				masterProblem.exportModel(path + "/masterProblem.lp");
			masterModel.setMasterObjective(masterObjective);
			masterModel.setCover(cover);
			masterModel.setAssignment(assignment);
			masterModel.setWeight(weight);
			masterModel.setxVar(x);
			masterModel.settVar(t);
			masterModel.setMasterProblem(masterProblem);
			masterModel.setxMap(xMap);
			masterModel.settMap(tMap);

		} catch (IloException exc) {
			System.err.println("Concert exception '" + exc + "' caught");
		}

	}

	private void generateType1InitialColumns() {
		for (Referee referee : data.getListOfReferees()) {
			for (ProposalPair pair : referee.getPairList()) {
				Column column = new Column();
				column.setColumnReferee(referee);
				List<ProposalPair> columnPairList = new ArrayList<>();
				columnPairList.add(pair);
				List<Proposal> columnProposalList = new ArrayList<>();
				columnProposalList.add(pair.getP1());
				columnProposalList.add(pair.getP2());
				column.setColumnPairList(columnPairList);
				column.setColumnProposalList(columnProposalList);
				listOfColumns.add(column);
			}
		}
	}

	private void generateType2InitialColumns() {
		for (Referee referee : data.getListOfReferees()) {
			Column column = new Column();
			column.setColumnReferee(referee);
			ProposalPair pair = referee.getPairList().get(0);
			List<ProposalPair> columnPairList = new ArrayList<>();
			columnPairList.add(pair);
			List<Proposal> columnProposalList = new ArrayList<>();
			columnProposalList.add(pair.getP1());
			columnProposalList.add(pair.getP2());
			column.setColumnPairList(columnPairList);
			column.setColumnProposalList(columnProposalList);
			listOfColumns.add(column);
		}
	}

	public Input getData() {
		return data;
	}

	public void setData(Input data) {
		this.data = data;
	}

	public List<Column> getListOfColumns() {
		return listOfColumns;
	}

	public void setListOfColumns(List<Column> listOfColumns) {
		this.listOfColumns = listOfColumns;
	}

	public MasterModel getMasterModel() {
		return masterModel;
	}

	public void setMasterModel(MasterModel masterModel) {
		this.masterModel = masterModel;
	}

	public Map<Integer, PricingModel> getPricingModels() {
		return pricingModels;
	}

	public void setPricingModels(Map<Integer, PricingModel> pricingModels) {
		this.pricingModels = pricingModels;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public double getBestIntegerValue() {
		return bestIntegerValue;
	}

	public void setBestIntegerValue(double bestIntegerValue) {
		this.bestIntegerValue = bestIntegerValue;
	}

	public boolean isDebug() {
		return debug;
	}

	public List<Output> getOpenNodes() {
		return openNodes;
	}

	public void setOpenNodes(List<Output> openNodes) {
		this.openNodes = openNodes;
	}

}
