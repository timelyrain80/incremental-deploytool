package pub.timelyrain.jenkins.plugin.increment;

import java.io.PrintStream;

public class RuntimeSetting {
    private String jobPath, workspacePath;
    private String bkRoot, prodRoot, packageRoot;
    private String[] regexList = null, replaceList = null;//changelog匹配表达式列表，替换值列表
    private String[] ignoreList = null; //忽略的文件列表 。比如生产环境的配置文件。
    private int buildNumber;
    private String jobName;
    private PrintStream log;
    private String shType;

    public RuntimeSetting(String bkRoot, String prodRoot, String packageRoot, String shType, String[] regexList, String[] replaceList, String[] ignoreList, String jenkinsHome, int buildNumber, String jobName, PrintStream log) {
        this.bkRoot = bkRoot;
        this.prodRoot = prodRoot;
        this.packageRoot = packageRoot;
        this.shType = shType;

        this.regexList = regexList;
        this.replaceList = replaceList;
        this.ignoreList = ignoreList;

        this.buildNumber = buildNumber;
        this.jobName = jobName;
        this.log = log;
        if(!jenkinsHome.endsWith("/") && !jenkinsHome.endsWith("\\"))
            jenkinsHome += "/";

        if (!bkRoot.endsWith("/") && !bkRoot.endsWith("\\"))
            this.bkRoot += "/";
        if (!prodRoot.endsWith("/") || !prodRoot.endsWith("\\"))
            this.prodRoot += "/";
        if (!packageRoot.endsWith("/") || !packageRoot.endsWith("\\"))
            this.packageRoot += "/";

        jobPath = jenkinsHome + "jobs/" + jobName + "/builds/" + buildNumber + "/";
        workspacePath = jenkinsHome + "workspace" + "/" + jobName + "/";

        log.println("脚本文件格式\t" + this.shType);
        log.println("构建编号\t" + this.buildNumber);
        log.println("预设的服务器上的 备份目录\t" + this.bkRoot);
        log.println("预设的服务器上的 应用部署目录\t" + this.prodRoot);
        log.println("预设的服务器上的 增量包解压目录\t" + this.packageRoot);
        log.println("");
        log.println("打包文件保存目录\t" + this.jobPath);
        log.println("编译输出目录\t" + this.workspacePath);
    }

    public String getJobPath() {
        return jobPath;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public String getBkRoot() {
        return bkRoot;
    }

    public String getProdRoot() {
        return prodRoot;
    }

    public String getPackageRoot() {
        return packageRoot;
    }

    public String[] getRegexList() {
        return regexList;
    }

    public String[] getReplaceList() {
        return replaceList;
    }

    public String[] getIgnoreList() {
        return ignoreList;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public String getJobName() {
        return jobName;
    }

    public PrintStream getLog() {
        return log;
    }

    public String getShType() {
        return shType;
    }
}
