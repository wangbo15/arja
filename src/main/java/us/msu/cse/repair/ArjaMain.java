package us.msu.cse.repair;

import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import org.eclipse.core.internal.resources.File;
import us.msu.cse.repair.algorithms.arja.Arja;
import us.msu.cse.repair.config.ProjectConfig;
import us.msu.cse.repair.core.AbstractRepairAlgorithm;
import us.msu.cse.repair.ec.operators.crossover.ExtendedCrossoverFactory;
import us.msu.cse.repair.ec.operators.mutation.ExtendedMutationFactory;
import us.msu.cse.repair.ec.problems.ArjaProblem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArjaMain {

	public static void main(String args[]) throws Exception {
		long start = System.currentTimeMillis();

		String bugID = args[0];
		assert bugID != null;
		System.out.println("Fixing " + bugID + " @ " + Main.EXECUTE_TIME_STR);
		ProjectConfig config = ProjectConfig.getInstance(bugID);

		List<String> argsList = new ArrayList<>();
		argsList.add("Arja");
		argsList.add("-DbugID");
		argsList.add(bugID);
		argsList.add("-DsrcJavaDir");
		argsList.add(config.getSrcJavaDir());
		argsList.add("-DbinJavaDir");
		argsList.add(config.getBinJavaDir());
		argsList.add("-DbinTestDir");
		argsList.add(config.getBinTestDir());
		argsList.add("-Ddependences");
		argsList.add(config.getDependencies());
		argsList.add("-DpatchOutputRoot");
		argsList.add("patches_" + bugID + "_" + Main.EXECUTE_TIME_STR);

		repair(argsList.toArray(new String[argsList.size()]), config);

		long end = System.currentTimeMillis();
		System.out.println("TIME: " + (end - start));
		System.out.println("TOTAL_COMPILATION_TIME: " + ArjaProblem.TOTAL_COMPILATION_TIME);
		System.out.println("COMPILATION_FAIL_TIME: " + ArjaProblem.COMPILATION_FAIL_TIME);
	}

	public static void repair(String args[], ProjectConfig config) throws Exception {

		HashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);
		HashMap<String, Object> parameters = Interpreter.getBasicParameterSetting(parameterStrs);

		// added by wb
		parameters.put("subject", config.getSubject());
		parameters.put("id", Integer.toString(config.getId()));
		parameters.put("rootDir", config.getRootDir());
		if(config.getSubject().equalsIgnoreCase("closure")) {
			parameters.put("useGzoltar", Boolean.toString(false));
		}

		String ingredientScreenerNameS = parameterStrs.get("ingredientScreenerName");
		if (ingredientScreenerNameS != null) {
			parameters.put("ingredientScreenerName", ingredientScreenerNameS);
		}

		int populationSize = 40;
		int maxGenerations = 50;
		
		String populationSizeS = parameterStrs.get("populationSize");
		if (populationSizeS != null)
			populationSize = Integer.parseInt(populationSizeS);
		
		String maxGenerationsS = parameterStrs.get("maxGenerations");
		if (maxGenerationsS != null)
			maxGenerations = Integer.parseInt(maxGenerationsS);

		// invoke several modules
		ArjaProblem problem = new ArjaProblem(parameters);
		AbstractRepairAlgorithm repairAlg = new Arja(problem);

		repairAlg.setInputParameter("populationSize", populationSize);
		repairAlg.setInputParameter("maxEvaluations", populationSize * maxGenerations);

		parameters = new HashMap<String, Object>();

		Crossover crossover;
		Mutation mutation;
		Selection selection;

		parameters = new HashMap<String, Object>();
		parameters.put("probability", 1.0);
		crossover = ExtendedCrossoverFactory.getCrossoverOperator("HUXSinglePointCrossover", parameters);

		parameters = new HashMap<String, Object>();
		parameters.put("probability", 1.0 / problem.getNumberOfModificationPoints());
		mutation = ExtendedMutationFactory.getMutationOperator("BitFilpUniformMutation", parameters);

		// Selection Operator
		parameters = null;
		selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		// Add the operators to the algorithm
		repairAlg.addOperator("crossover", crossover);
		repairAlg.addOperator("mutation", mutation);
		repairAlg.addOperator("selection", selection);
		
		repairAlg.execute();
	}

}
