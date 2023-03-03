package us.msu.cse.repair;

import jmetal.util.JMException;
import us.msu.cse.repair.config.ProjectConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class Main {
	public static final Date EXECUTE_TIME = new Date();
	public static String EXECUTE_TIME_STR;
	static {
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		EXECUTE_TIME_STR = df.format(EXECUTE_TIME);
	}

	private static String[] initArgs(String[] args, ProjectConfig config) {
		List<String> newArgs = new ArrayList<>(Arrays.asList(args));
		newArgs.add("-DsrcJavaDir");
		newArgs.add(config.getSrcJavaDir());
		newArgs.add("-DbinJavaDir");
		newArgs.add(config.getBinJavaDir());
		newArgs.add("-DbinTestDir");
		newArgs.add(config.getBinTestDir());
		newArgs.add("-Ddependences");
		newArgs.add(config.getDependencies());
		newArgs.add("-DpatchOutputRoot");

		String bugID = args[1]; // args[0] is the name of repair approach
		newArgs.add("patches_" + bugID + "_" + Main.EXECUTE_TIME_STR);
		return newArgs.toArray(new String[newArgs.size()]);
	}

	/**
	 * The main entrance
	 * @param args:
	 *            args[0] must be the repair approach
	 *            args[1] must be the bug id, like `closure_1`
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		if (args.length < 2) {
			throw new Error();
		}
		String bugID = args[1];
		if (!ProjectConfig.isLegalBugID(bugID)) {
			throw new Error(bugID);
		}
		System.out.println("Fixing " + bugID + " @ " + Main.EXECUTE_TIME_STR);
		ProjectConfig config = ProjectConfig.getInstance(bugID);

		args = initArgs(args, config);
		if (args[0].equalsIgnoreCase("Arja"))
			ArjaMain.repair(args, config);
		else if (args[0].equalsIgnoreCase("GenProg"))
			GenProgMain.main(args);
		else if (args[0].equalsIgnoreCase("RSRepair"))
			RSRepairMain.main(args);
		else if (args[0].equalsIgnoreCase("Kali"))
			KaliMain.main(args);
		else if (args[0].equalsIgnoreCase("-listParameters"))
			ParameterInfoMain.main(args);
		else {
			throw new JMException("The repair approach " + args[0] + " does not exist!");
		}
	}
}