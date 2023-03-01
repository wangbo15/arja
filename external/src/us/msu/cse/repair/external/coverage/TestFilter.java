package us.msu.cse.repair.external.coverage;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import us.msu.cse.repair.external.util.Util;


/**
 * This class employs jacoco to instrument and trace source.
 */
public class TestFilter {

	private final static int STEP = 500;

	private static String binJavaDir;
	private static String binTestDir;
	private static Set<String> dependences;

	private static Set<LCNode> faultyLines;
	private static Set<String> orgPositiveTests;

	/** Used to collect coverage info */
	private static Set<String> faultyClasses;

	private static int value = -1;

	public static void main(String args[]) throws Exception  {
		binJavaDir = args[0].trim();
		binTestDir = args[1].trim();
		dependences = new HashSet<String>();

		faultyLines = new HashSet<LCNode>();
		faultyClasses = new HashSet<String>();
		orgPositiveTests = new HashSet<String>();

		String[] depStrs = args[2].trim().split(File.pathSeparator);
		for (String dep : depStrs)
			dependences.add(dep);

		if (args[3].startsWith("@")) {
			String path = args[3].trim().substring(1);
			List<String> lineStrs = Util.readLines(new File(path));
			for (String str : lineStrs) {
				String ns[] = str.trim().split("#");
				LCNode node = new LCNode(ns[0], Integer.parseInt(ns[1]));
				faultyLines.add(node);
				faultyClasses.add(ns[0]);
			}
		} else {
			String fls[] = args[3].trim().split(File.pathSeparator);
			for (String str : fls) {
				String ns[] = str.split("#");
				LCNode node = new LCNode(ns[0], Integer.parseInt(ns[1]));
				faultyLines.add(node);
				faultyClasses.add(ns[0]);
			}
		}

		if (args[4].startsWith("@")) {
			String path = args[4].trim().substring(1);
			List<String> lineStrs = Util.readLines(new File(path));
			for (String str : lineStrs)
				orgPositiveTests.add(str.trim());
		} else {
			String pts[] = args[4].trim().split(File.pathSeparator);
			for (String str : pts)
				orgPositiveTests.add(str);
		}

		if (args.length == 6) {
			value = Integer.valueOf(args[5].trim());
		}

		List<String> filteredPositiveTests;
		if (value < 0) {
			// for closure, filter only 500 tests
			filteredPositiveTests = getFilteredPositiveTests();
		} else {
			filteredPositiveTests = getFilteredPositiveTestsBySteps();
		}
		printFilteredPositiveTests(filteredPositiveTests);
		System.exit(0);
	}

	private static void printFilteredPositiveTests(List<String> filteredPositiveTests) {
		System.out.println();
		for (String test : filteredPositiveTests) {
			System.out.println("FilteredTest: " + test);
		}
	}

	private static List<String> getFilteredPositiveTests() throws Exception {
		List<String> filteredPositiveTests = new ArrayList<String>();
		for (String test : orgPositiveTests) {
			if (!canFiltered(test))
				filteredPositiveTests.add(test);
		}

		return filteredPositiveTests;
	}

	private static List<String> getFilteredPositiveTestsBySteps() throws Exception {
		List<String> filteredPositiveTests = new ArrayList<String>();
		int step = 0;
		for (String test : orgPositiveTests) {
			step += 1;
			if (step >= value) {
				if (!canFiltered(test)) {
					filteredPositiveTests.add(test);
					System.out.println("Add>>>>>>" + test);
				}
			}
			if(step >= value + STEP || step > orgPositiveTests.size()){
				break;
			}

		}

		return filteredPositiveTests;
	}

	private static boolean canFiltered(String test)
			throws Exception {
		System.out.println(">>>> Processing >>>> " + test);
		final MemoryClassLoader loader = new MemoryClassLoader(Util.getURLs(binJavaDir, binTestDir, dependences));

		final IRuntime runtime = new LoggerRuntime();
		final Instrumenter instr = new Instrumenter(runtime);

		for (String binJavaClass : faultyClasses) {
			InputStream istream = Util.getTargetClass(binJavaDir, binJavaClass);
			byte[] bytes = instr.instrument(istream, binJavaClass);
			istream.close();
			loader.addDefinition(binJavaClass, bytes);
		}

		RuntimeData data = new RuntimeData();
		runtime.startup(data);

		String clsName = test.split("#")[0];
		String mName = test.split("#")[1];
		Request request = Request.method(loader.loadClass(clsName), mName);
		new JUnitCore().run(request);

		final ExecutionDataStore executionData = new ExecutionDataStore();
		final SessionInfoStore sessionInfos = new SessionInfoStore();
		data.collect(executionData, sessionInfos, false);
		runtime.shutdown();

		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

		for (String binJavaClass : faultyClasses) {
			InputStream istream = Util.getTargetClass(binJavaDir, binJavaClass);
			analyzer.analyzeClass(istream, binJavaClass);
			istream.close();
		}

		loader.close();

		for (final IClassCoverage cc : coverageBuilder.getClasses()) {
			String className = cc.getName().replace("/", ".");
			for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
				int status = cc.getLine(i).getStatus();
				if (status == ICounter.FULLY_COVERED || status == ICounter.PARTLY_COVERED) {
					LCNode lcNode = new LCNode(className, i);
					if (faultyLines.contains(lcNode)) {
						return false;
					}
				}
			}
		}

//		System.out.println("end!");
		return true;
	}

}
