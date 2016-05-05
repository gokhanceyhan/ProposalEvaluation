import java.util.Map;

import ilog.concert.*;
import ilog.cplex.*;

public class PricingModel {

	public PricingModel() {

	}

	private IloCplex pricingProblem;
	private IloIntVar[] aVar;
	private IloIntVar[] xVar;
	private Map<String, IloIntVar> aMap;
	private Map<String, Double> objWeight;

	public IloCplex getPricingProblem() {
		return pricingProblem;
	}

	public void setPricingProblem(IloCplex pricingProblem) {
		this.pricingProblem = pricingProblem;
	}

	public IloIntVar[] getaVar() {
		return aVar;
	}

	public void setaVar(IloIntVar[] aVar) {
		this.aVar = aVar;
	}

	public IloIntVar[] getxVar() {
		return xVar;
	}

	public void setxVar(IloIntVar[] xVar) {
		this.xVar = xVar;
	}

	public Map<String, IloIntVar> getaMap() {
		return aMap;
	}

	public void setaMap(Map<String, IloIntVar> aMap) {
		this.aMap = aMap;
	}

	public Map<String, Double> getObjWeight() {
		return objWeight;
	}

	public void setObjWeight(Map<String, Double> objWeight) {
		this.objWeight = objWeight;
	}

}
