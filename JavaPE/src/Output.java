import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.CplexStatus;

public class Output {

	private int level;
	private int nodeNumber;
	private String status;
	private String tVarStatus;
	private double objectiveValue;
	private Map<String, Double> columnValues;
	private Map<String, Double> tValues;

	public Output(int level, int nodeNumber) {
		this.level = level;
		this.nodeNumber = nodeNumber;
		this.status = "";
		this.tVarStatus = "";
		this.objectiveValue = 0.0;
		this.columnValues = new HashMap<>();
		this.tValues = new HashMap<>();
	}

	public void fillOutputValues(MasterModel masterModel) throws IloException {
		CplexStatus cplexStatus = masterModel.getMasterProblem().getCplexStatus();
		if (cplexStatus != CplexStatus.Infeasible) {
			objectiveValue = masterModel.getMasterProblem().getObjValue();
			fetchColumnValuesFromMasterProblem(masterModel);
			fetchTValuesFromMasterProblem(masterModel);
			isIntegerFeasible();
			findTVarStatus();
		} else
			status = cplexStatus.toString();
	}

	private void isIntegerFeasible() {
		status = "IntFeasible";
		for (Map.Entry<String, Double> columnId : columnValues.entrySet()) {
			if (columnId.getValue() > Constants.intSolTolerance
					&& columnId.getValue() < 1 - Constants.intSolTolerance) {
				status = "Feasible";
				break;
			}
		}
	}

	private void findTVarStatus() {
		tVarStatus = "Integer";
		for (Map.Entry<String, Double> tVar : tValues.entrySet()) {
			if (tVar.getValue() > Constants.intSolTolerance && tVar.getValue() < 1 - Constants.intSolTolerance) {
				tVarStatus = "Fractional";
				break;
			}
		}
	}

	private void fetchColumnValuesFromMasterProblem(MasterModel masterModel) throws IloException {
		for (String xName : masterModel.getxMap().keySet()) {
			double xValue = masterModel.getMasterProblem().getValue(masterModel.getxMap().get(xName));
			columnValues.put(xName, xValue);
		}
	}

	private void fetchTValuesFromMasterProblem(MasterModel masterModel) throws IloException {
		for (Map.Entry<String, IloNumVar> t : masterModel.gettMap().entrySet()) {
			double tValue = masterModel.getMasterProblem().getValue(t.getValue());
			tValues.put(t.getKey(), tValue);
		}
	}

	public double getObjectiveValue() {
		return objectiveValue;
	}

	public void setObjectiveValue(double objectiveValue) {
		this.objectiveValue = objectiveValue;
	}

	public Map<String, Double> getColumnValues() {
		return columnValues;
	}

	public void setColumnValues(Map<String, Double> columnValues) {
		this.columnValues = columnValues;
	}

	public Map<String, Double> gettValues() {
		return tValues;
	}

	public void settValues(Map<String, Double> tValues) {
		this.tValues = tValues;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getNodeNumber() {
		return nodeNumber;
	}

	public void setNodeNumber(int nodeNumber) {
		this.nodeNumber = nodeNumber;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String gettVarStatus() {
		return tVarStatus;
	}

	public void settVarStatus(String tVarStatus) {
		this.tVarStatus = tVarStatus;
	}

}
