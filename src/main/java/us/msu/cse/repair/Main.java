package us.msu.cse.repair;

import jmetal.util.JMException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Main {
	public static final Date EXECUTE_TIME = new Date();
	public static String EXECUTE_TIME_STR;
	static {
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		EXECUTE_TIME_STR = df.format(EXECUTE_TIME);
	}

	public static void main(String args[]) throws Exception {
		if (args[0].equalsIgnoreCase("Arja"))
			ArjaMain.main(args);
		else if (args[0].equalsIgnoreCase("GenProg"))
			GenProgMain.main(args);
		else if (args[0].equalsIgnoreCase("RSRepair"))
			RSRepairMain.main(args);
		else if (args[0].equalsIgnoreCase("Kali"))
			KaliMain.main(args);
		else if (args[0].equalsIgnoreCase("-listParameters"))
			ParameterInfoMain.main(args);
		else {
			throw new JMException("The repair apporach " + args[0] + " does not exist!");
		}
	}
}