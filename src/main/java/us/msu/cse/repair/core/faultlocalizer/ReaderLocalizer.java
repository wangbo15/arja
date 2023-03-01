package us.msu.cse.repair.core.faultlocalizer;

import org.apache.commons.io.FileUtils;
import us.msu.cse.repair.config.ProjectConfig;
import us.msu.cse.repair.core.parser.LCNode;
import us.msu.cse.repair.core.util.visitors.CMD;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReaderLocalizer implements IFaultLocalizer{

    private String subject;

    private String id;

    private boolean isManual;

    private Set<String> positiveTestMethods;
    private Set<String> negativeTestMethods;

    private Map<LCNode, Double> faultyLines;

    public ReaderLocalizer(String subject, int id, String rootDir, boolean isManual){
        this.subject = subject;
        this.id = Integer.toString(id);
        this.isManual = isManual;
        String str = isManual ? "manual" : "ochiai";
        System.out.println("Loading " + str + " FL data...");
        File suspiciousFile = new File("location/" + str + "/" + subject + "/" + id + ".txt");
        assert suspiciousFile.exists(): "FILE DOES NOT EXIST: " + suspiciousFile.getAbsolutePath();
        try {
            List<String> lines = FileUtils.readLines(suspiciousFile);
            faultyLines = new HashMap<LCNode, Double>();
            for (String line: lines) {
                String[] columns = line.split(",");
                String key = columns[0];
                Double suspValue = Double.valueOf(columns[1]);
                if (suspValue > 0.0D) {
                    //key: ClassName#LineNumber
                    String[] arr = key.split("#");
                    String cls = arr[0];
                    int lineNumber = Integer.valueOf(arr[1]);
                    LCNode node = new LCNode(cls, lineNumber);
                    faultyLines.put(node, suspValue);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File allTestFile = new File(rootDir + "/all-tests.txt");
        if (!allTestFile.exists() ){
            allTestFile = new File(rootDir + "/all_tests");
        }
        assert allTestFile.exists(): allTestFile;


        // adding `positiveTestMethods` and `negativeTestMethods`
        File failingTestFile = new File(rootDir + "/failing_tests");
        assert allTestFile.exists() && failingTestFile.exists();
        positiveTestMethods = new HashSet<String>();
        negativeTestMethods = new HashSet<String>();
        try {
            List<String> allTestLines = FileUtils.readLines(allTestFile);
            List<String> failingTestLines = FileUtils.readLines(failingTestFile);

            for(String line: allTestLines) {
                if(line.indexOf('[') >= 0) {
                    continue;
                }
                String[] arr = line.split("\\(");
                String testMtd = arr[0];
                String testCls = arr[1].substring(0, arr[1].length() - 1); // remove the last `)`
                //
                positiveTestMethods.add(testCls + "#" + testMtd);
            }
            assert !positiveTestMethods.isEmpty();
            for(String line: failingTestLines) {
                if(line.startsWith("--- ")) {
                    String test = line.substring(4, line.length());
                    test = test.replaceAll("::", "#");
                    negativeTestMethods.add(test);
                }
            }
            assert !negativeTestMethods.isEmpty();
            positiveTestMethods.removeAll(negativeTestMethods);

            /*if(subject.equalsIgnoreCase("closure")) {
                // TODO: only read from d4j base for closure
                String lower = subject.toLowerCase();
                String headCapitalized = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
                String path = ProjectConfig.DEFECTS4J_HOME + "framework/projects/" + headCapitalized +
                        "/relevant_tests/" + id;
                Set<String> allRelatedClass = new HashSet<>(FileUtils.readLines(new File(path)));
                List<String> tobeRemoved = new ArrayList<>(positiveTestMethods.size());
                for (String positiveTest: positiveTestMethods) {
                    String cls = positiveTest.split("#")[0];
                    if (!allRelatedClass.contains(cls)) {
                        tobeRemoved.add(positiveTest);
                    }
                }
                positiveTestMethods.removeAll(tobeRemoved);
            }*/

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ReaderLocalizer(ProjectConfig config, boolean manual){
        this(config.getSubject(), config.getId(), config.getRootDir(), manual);
    }

    @Override
    public Map<LCNode, Double> searchSuspicious(double thr) {
        Map<LCNode, Double> partFaultyLines = new HashMap<LCNode, Double>();
        for (Map.Entry<LCNode, Double> entry : faultyLines.entrySet()) {
            if (entry.getValue() >= thr)
                partFaultyLines.put(entry.getKey(), entry.getValue());
        }
        return partFaultyLines;
    }

    @Override
    public Set<String> getPositiveTests() {
        return this.positiveTestMethods;
    }

    @Override
    public Set<String> getNegativeTests() {
        return this.negativeTestMethods;
    }

    public String getSubject() {
        return subject;
    }

    public String getId() {
        return id;
    }

    public boolean isManual() {
        return isManual;
    }
}
