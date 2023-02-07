package us.msu.cse.repair.config;

import us.msu.cse.repair.core.util.visitors.CMD;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ProjectConfig {

    public static String DEFECTS4J_HOME = "";
    static {
        try {
            List<String> res = CMD.run(Arrays.asList("which", "defects4j"), new File("."));
            assert res.size() == 1;
            DEFECTS4J_HOME = res.get(0);
            assert DEFECTS4J_HOME.contains("defects4j");
            int start = DEFECTS4J_HOME.indexOf("defects4j");
            DEFECTS4J_HOME = DEFECTS4J_HOME.substring(0, start) + "defects4j/";
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** such as `chart_3` */
    private String bugName;

    /** such as `chart` */
    private String subject;
    /** such as `3` */
    private int id;

    private String rootDir;
    private File rootFile;
    private String srcJavaDir;
    private String binJavaDir;
    private String binTestDir;

    private File allTestFile;
    private File failingTestFile;

    public ProjectConfig(String bugName, String rootDir, String srcJavaDir, String binJavaDir, String binTestDir) {
        assert bugName != null;
        assert bugName.indexOf('_') >= 0;
        assert Character.isLowerCase(bugName.charAt(0));
        assert rootDir != null;
        assert srcJavaDir != null;
        assert binJavaDir != null;
        assert binTestDir != null;

        this.bugName = bugName;
        String[] arr = bugName.split("_");
        this.subject = arr[0];
        this.id = Integer.valueOf(arr[1]);
        this.rootDir = rootDir;
        this.rootFile = new File(rootDir);
        assert rootFile.exists();

        this.srcJavaDir = srcJavaDir;
        this.binJavaDir = binJavaDir;
        this.binTestDir = binTestDir;

        if(System.getProperty("os.name").toLowerCase().contains("linux")) {
            allTestFile = new File(this.rootDir + "/all-tests.txt");
        } else {
            allTestFile = new File(this.rootDir + "/all_tests");
        }
        failingTestFile = new File(this.getRootDir() + "/failing_tests");
        if(!allTestFile.exists() || !failingTestFile.exists()) {
            this.d4jCompile();
            this.d4jTest();
            assert allTestFile.exists();
            assert failingTestFile.exists();
        }
    }

    public String getBugName() {
        return bugName;
    }

    public void setBugName(String bugName) {
        this.bugName = bugName;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public File getRootFile() {
        return rootFile;
    }

    public void setRootFile(File rootFile) {
        this.rootFile = rootFile;
    }

    public String getSrcJavaDir() {
        return srcJavaDir;
    }

    public void setSrcJavaDir(String srcJavaDir) {
        this.srcJavaDir = srcJavaDir;
    }

    public String getBinJavaDir() {
        return binJavaDir;
    }

    public void setBinJavaDir(String binJavaDir) {
        this.binJavaDir = binJavaDir;
    }

    public String getBinTestDir() {
        return binTestDir;
    }

    public void setBinTestDir(String binTestDir) {
        this.binTestDir = binTestDir;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void d4jCompile(){
        try {
            List<String> params = new ArrayList<>();
            params.add("defects4j");
            params.add("compile");
            CMD.run(params, this.rootFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void d4jTest() {
        try {
            List<String> params = new ArrayList<>();
            params.add("defects4j");
            params.add("test");
            CMD.run(params, this.rootFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanD4jOutput(){
        if(allTestFile.exists()) {
            allTestFile.delete();
        }
        if(failingTestFile.exists()) {
            failingTestFile.delete();
        }
    }

    @Override
    public String toString() {
        return "ProjectConfig{" +
                "\n bugName='" + bugName + '\'' +
                "\n rootDir='" + rootDir + '\'' +
                "\n srcJavaDir='" + srcJavaDir + '\'' +
                "\n binJavaDir='" + binJavaDir + '\'' +
                "\n binTestDir='" + binTestDir + '\'' +
                "\n}";
    }

    public String getDependencies(){
        if(!subject.equalsIgnoreCase("closure")) {
            return "/home/bjtucs/program_files/defects4j/framework/projects/lib/junit-4.11.jar";
        }
        File libFile = new File(this.rootDir + "/lib");
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };
        String clsPath = new File(this.rootDir + "/build/lib/rhino.jar").getAbsolutePath();
        for(File file: libFile.listFiles(filter)) {
            clsPath += ":" + file.getAbsolutePath();
        }
        return clsPath;
    }

    public static ProjectConfig getInstance(String bugID) {

        String d4jRoot = System.getenv("D4JSrcRoot");
        assert d4jRoot != null: "The evn variable D4JSrcRoot is null!";
        assert new File(d4jRoot).exists(): d4jRoot;

        bugID = bugID.toLowerCase();
        String[] arr = bugID.split("_");
        assert arr.length == 2;
        String proj = arr[0];
        int id = Integer.valueOf(arr[1]);

        File rootFile = new File(d4jRoot + "/" + proj + "/" + bugID + "_buggy");
        assert rootFile.exists();
        String rootPath = rootFile.getAbsolutePath();

        String srcJavaDir = null, binJavaDir = null, binTestDir = null;
        switch (proj) {
            case "chart": {
                assert id >=1 && id <=26;
                srcJavaDir = rootPath + "/source/";
                binJavaDir = rootPath + "/build/";
                binTestDir = rootPath + "/build-tests/";
                break;
            }
            case "lang": {
                assert id >=1 && id <= 65;
                srcJavaDir = rootPath + "/src/main/java/";
                binJavaDir = rootPath + "/target/classes/";
                binTestDir = rootPath + "/target/tests/";
                break;
            }
            case "math": {
                assert id >=1 && id <= 106;
                if(id <= 84) {
                    srcJavaDir = rootPath + "/src/main/java/";
                } else {
                    srcJavaDir = rootPath + "/src/java/";
                }
                binJavaDir = rootPath + "/target/classes/";
                binTestDir = rootPath + "/target/test-classes/";
                break;
            }
            case "time": {
                assert id >=1 && id <= 27;
                srcJavaDir = rootPath + "/src/main/java/";
                if (id <= 11) {
                    binJavaDir = rootPath + "/target/classes/";
                    binTestDir = rootPath + "/target/test-classes/";
                } else {
                    binJavaDir = rootPath + "/build/classes/";
                    binTestDir = rootPath + "/build/tests/";
                }
                break;
            }
            case "closure": {
                assert id >=1 && id <= 133;
                srcJavaDir = rootPath + "/src/";
                binJavaDir = rootPath + "/build/classes/";
                binTestDir = rootPath + "/build/test/";
                break;
            }
            default:{
                throw new Error("ERROR PROJECT NAME: " + bugID);
            }
        }
        assert srcJavaDir != null && new File(srcJavaDir).exists();

        File srcFile = new File(srcJavaDir);
        File binFile = new File(binJavaDir);
        File testBinFile = new File(binTestDir);
        if(!binFile.exists() || !testBinFile.exists()) {
            try {
                List<String> params = new ArrayList<>();
                params.add("defects4j");
                params.add("compile");
                CMD.run(params, rootFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        assert srcFile.exists();
        assert binFile.exists();
        assert testBinFile.exists();
        return new ProjectConfig(bugID, rootFile.getAbsolutePath(), srcFile.getAbsolutePath(), binFile.getAbsolutePath(), testBinFile.getAbsolutePath());
    }
}
