package pub.timelyrain.jenkins.plugin.increment;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class IncrementalBuilder extends Builder implements SimpleBuildStep {
    private String srcRoot, webRoot;
    private String bkRoot, prodRoot, packageRoot; //在目标服务器上的备份路径、发布目的地、增量包解压路径
    private String shType;//脚本文件格式
    private String regexStrs, replaceStrs, ignoreStrs;

    private String[] regexList = null, replaceList = null;//changelog匹配表达式列表，替换值列表
    private String[] ignoreList = null; //忽略的文件列表 。比如生产环境的配置文件。
    private int buildNumber;
    private String jobName;
    private PrintStream LOG;


    @DataBoundConstructor
    public IncrementalBuilder(String srcRoot, String webRoot, String bkRoot, String prodRoot, String packageRoot, String shType, String regexStrs, String replaceStrs, String ignoreStrs) {
        this.srcRoot = srcRoot;
        this.webRoot = webRoot;

        this.bkRoot = bkRoot;
        this.prodRoot = prodRoot;
        this.packageRoot = packageRoot;
        this.shType = shType;


        this.regexStrs = regexStrs;
        this.replaceStrs = replaceStrs;
        this.ignoreStrs = ignoreStrs;

        if (regexStrs != null && !"".equalsIgnoreCase(regexStrs)) {
            regexList = regexStrs.split("\n");
        }
        if (replaceStrs != null && !"".equalsIgnoreCase(replaceStrs)) {
            replaceList = replaceStrs.split("\n");
        }
        if (ignoreStrs != null && !"".equalsIgnoreCase(ignoreStrs)) {
            ignoreList = ignoreStrs.split("\n");
        }
    }


    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        EnvVars envs = run.getEnvironment(listener);
        jobName = envs.get("JOB_NAME");
        buildNumber = run.getNumber();
        String homePath = workspace.absolutize().getParent().getParent().getRemote() + "/";

        LOG = listener.getLogger();

        LOG.println("-----------------------------------------------------------------");
        LOG.println("增量打包插件执行中...");
        LOG.println("github: https://github.com/timelyrain80/incremental-deploytool.git");
        LOG.println("-----------------------------------------------------------------");
        LOG.println("");

        //调用打包程序
        RuntimeSetting rs = new RuntimeSetting(srcRoot, webRoot, bkRoot, prodRoot, packageRoot, shType, regexList, replaceList, ignoreList, homePath, buildNumber, jobName, listener.getLogger());
        PackageTools pk = new PackageTools(rs);
        pk.makePackage();

        LOG.println("插件执行完毕");
    }


    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public List<String> getShTypes() {
            ArrayList<String> list = new ArrayList<>();
            list.add("linux");
            list.add("windows");
            return list;
        }

        public String getRegs() {
            return "";
        }

        public String getReplaces() {
            return "";
        }
        //检查设置参数是否符合规则
//        public FormValidation doCheckSrcPath(@QueryParameter String srcPath) {
//            if (isNull(srcPath))
//                return FormValidation.error("必须输入java src路径");
//            return FormValidation.ok();
//        }
//
//        public FormValidation doCheckCompileTo(@QueryParameter String compileTo) {
//            if (isNull(compileTo)) return FormValidation.error("必须输入java 编译输出路径");
//            return FormValidation.ok();
//        }

        public FormValidation doCheckBkRoot(@QueryParameter String bkRoot) {
            if (isNull(bkRoot)) return FormValidation.error("必须设置应用服务器上的备份目录");
            return FormValidation.ok();
        }

        public FormValidation doCheckProdRoot(@QueryParameter String prodRoot) {
            if (isNull(prodRoot)) return FormValidation.error("必须设置应用服务器上的应用部署目录");

            return FormValidation.ok();
        }

        public FormValidation doCheckPackageRoot(@QueryParameter String packageRoot) {
            if (isNull(packageRoot)) return FormValidation.error("必须设置应用服务器上的增量包解压目录");

            return FormValidation.ok();
        }

//        public FormValidation doCheckRegexStrs(@QueryParameter String regexStrs) {
//            int regCount = 0, replaceCount = 0;
//            if (!isNull(regexStrs)) {
//                regexList = regexStrs.split("\n");
//                regCount = regexList.length;
//            }
//
//            if (replaceList == null)
//                replaceCount = 0;
//            else
//                replaceCount = replaceList.length;
//            if (regCount != replaceCount)
//                return FormValidation.error("查找表达式与替换内容行数必须一致");
//
//            return FormValidation.ok();
//        }
//
//        public FormValidation doCheckReplaceStrs(@QueryParameter String replaceStrs) {
//            int regCount = 0, replaceCount = 0;
//            if (!isNull(replaceStrs)) {
//                replaceList = replaceStrs.split("\n");
//                replaceCount = replaceList.length;
//            }
//
//            if (regexList == null)
//                regCount = 0;
//            else
//                regCount = regexList.length;
//
//            if (regCount != replaceCount)
//                return FormValidation.error("查找表达式与替换内容行数必须一致");
//
//            return FormValidation.ok();
//        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "打包增量发布文件";
        }

        private boolean isNull(String str) {
            return str == null || "".equalsIgnoreCase(str);
        }
    }

    public String getSrcRoot() {
        return srcRoot;
    }

    @DataBoundSetter
    public void setSrcRoot(String srcRoot) {
        this.srcRoot = srcRoot;
    }

    public String getWebRoot() {
        return webRoot;
    }

    @DataBoundSetter
    public void setWebRoot(String webRoot) {
        this.webRoot = webRoot;
    }

    public String getBkRoot() {
        return bkRoot;
    }

    @DataBoundSetter
    public void setBkRoot(String bkRoot) {
        this.bkRoot = bkRoot;
    }

    public String getProdRoot() {
        return prodRoot;
    }

    @DataBoundSetter
    public void setProdRoot(String prodRoot) {
        this.prodRoot = prodRoot;
    }

    public String getPackageRoot() {
        return packageRoot;
    }

    @DataBoundSetter
    public void setPackageRoot(String packageRoot) {
        this.packageRoot = packageRoot;
    }

//    public String getSrcPath() {
//        return srcPath;
//    }
//
//    @DataBoundSetter
//    public void setSrcPath(String srcPath) {
//        this.srcPath = srcPath;
//    }
//
//    public String getCompileTo() {
//        return compileTo;
//    }
//
//    @DataBoundSetter
//    public void setCompileTo(String compileTo) {
//        this.compileTo = compileTo;
//    }

    public String getShType() {
        return shType;
    }

    @DataBoundSetter
    public void setShType(String shType) {
        this.shType = shType;
    }

    public String getRegexStrs() {
        return regexStrs;
    }

    @DataBoundSetter
    public void setRegexStrs(String regexStrs) {
        this.regexStrs = regexStrs;
    }

    public String getReplaceStrs() {
        return replaceStrs;
    }

    @DataBoundSetter
    public void setReplaceStrs(String replaceStrs) {
        this.replaceStrs = replaceStrs;
    }

    public String getIgnoreStrs() {
        return ignoreStrs;
    }

    @DataBoundSetter
    public void setIgnoreStrs(String ignoreStrs) {
        this.ignoreStrs = ignoreStrs;
    }


}
