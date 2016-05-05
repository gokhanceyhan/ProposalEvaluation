import java.util.List;
import java.util.Map;

import ilog.concert.*;
import ilog.cplex.*;

public class MasterModel {

	public MasterModel() {
		
	}
	private IloCplex masterProblem;
	private IloObjective masterObjective;
	private IloRange[] cover;
	private IloRange[] assignment;
	private IloRange[] weight;
	private IloNumVar[] xVar;
	private IloNumVar[] tVar;
	private Map<String, IloNumVar> xMap;
	private Map<String, IloNumVar> tMap;
	
	public IloCplex getMasterProblem() {
		return masterProblem;
	}
	public void setMasterProblem(IloCplex masterProblem) {
		this.masterProblem = masterProblem;
	}
	public IloObjective getMasterObjective() {
		return masterObjective;
	}
	public void setMasterObjective(IloObjective masterObjective) {
		this.masterObjective = masterObjective;
	}
	public IloRange[] getCover() {
		return cover;
	}
	public void setCover(IloRange[] cover) {
		this.cover = cover;
	}
	public IloRange[] getAssignment() {
		return assignment;
	}
	public void setAssignment(IloRange[] assignment) {
		this.assignment = assignment;
	}
	public IloNumVar[] getxVar() {
		return xVar;
	}
	public void setxVar(IloNumVar[] xVar) {
		this.xVar = xVar;
	}
	public IloRange[] getWeight() {
		return weight;
	}
	public void setWeight(IloRange[] weight) {
		this.weight = weight;
	}
	public IloNumVar[] gettVar() {
		return tVar;
	}
	public void settVar(IloNumVar[] tVar) {
		this.tVar = tVar;
	}
	public void addColumnsToMasterProblem(Input data, List<Column> columnList, int numOfColumns){
		try{
			IloNumVar[] addXVars = new IloNumVar[columnList.size()];
			for(int i=0;i<columnList.size();i++){
				String xName = "x_"+(numOfColumns+i+1);
				double[] coverCoeffs = Common.createCoverCoefficientsForXVars(columnList.get(i), data.getListOfProposalPairs());
				double[] assignmentCoeffs = Common.createAssignmentCoefficientsForXVars(columnList.get(i), data.getListOfReferees());
				IloColumn xColumn = masterProblem.column(masterObjective, 0.0);
				for (int c = 0; c < data.getListOfProposalPairs().size(); c++ ) {
					xColumn=xColumn.and(masterProblem.column(cover[c], coverCoeffs[c]));
				}
				for(int a = 0; a < data.getListOfReferees().size(); a++ ) {
					xColumn=xColumn.and(masterProblem.column(assignment[a], assignmentCoeffs[a]));
				}
				for(int w = 0; w < data.getListOfProposalPairs().size(); w++ ) {
					xColumn=xColumn.and(masterProblem.column(weight[w], 0.0));
				}
				addXVars[i] = masterProblem.numVar(xColumn, 0.0, 1.0, xName);
				xMap.put(xName, addXVars[i]);
			}
			IloNumVar[] allXVars = new IloNumVar[xVar.length+columnList.size()];
			for(int i=0;i<xVar.length;i++){
				allXVars[i]= xVar[i];
			}
			for(int i=xVar.length;i<allXVars.length;i++){
				allXVars[i]=addXVars[i-xVar.length];
			}
			setxVar(allXVars);
		}
		catch ( IloException exc ) {
			System.err.println("Concert exception '" + exc + "' caught");
		}
		
		
		
	}
	public Map<String, IloNumVar> getxMap() {
		return xMap;
	}
	public void setxMap(Map<String, IloNumVar> xMap) {
		this.xMap = xMap;
	}
	public Map<String, IloNumVar> gettMap() {
		return tMap;
	}
	public void settMap(Map<String, IloNumVar> tMap) {
		this.tMap = tMap;
	}

}
