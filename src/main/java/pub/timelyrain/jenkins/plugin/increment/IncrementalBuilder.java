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
import java.util.List;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class IncrementalBuilder extends Builder implements SimpleBuildStep {
    private String bkRoot, prodRoot, packageRoot; //在目标服务器上的备份路径、发布目的地、增量包解压路径
    private String srcPath, compileTo;       //源代码路径,编译输出路径
    private String shType;//脚本文件格式
    private String regexStrs, replaceStrs, ignoreStrs;
    public static String[] REGEXS = null, REPLACES = null;//changelog匹配表达式列表，替换值列表
    public static String[] IGNORES = null; //忽略的文件列表 。比如生产环境的配置文件。
    public static int BUILDNUMBER;
    public static String JOBNAME;
    public static PrintStream LOG;

    @DataBoundConstructor
    public IncrementalBuilder(String srcPath, String compileTo, String bkRoot, String prodRoot, String packageRoot, String shType, String regexStrs, String replaceStrs, String ignoreStrs) {
        this.srcPath = srcPath;
        this.compileTo = compileTo;

        this.bkRoot = bkRoot;
        this.prodRoot = prodRoot;
        this.packageRoot = packageRoot;
        this.shType = shType;

        this.regexStrs = regexStrs;
        this.replaceStrs = replaceStrs;
        this.ignoreStrs = ignoreStrs;

        if (regexStrs != null && !"".equalsIgnoreCase(regexStrs)) {
            REGEXS = regexStrs.split("\n");
        }
        if (replaceStrs != null && !"".equalsIgnoreCase(replaceStrs)) {
            REPLACES = replaceStrs.split("\n");
        }
        if (ignoreStrs != null && !"".equalsIgnoreCase(ignoreStrs)) {
            IGNORES = ignoreStrs.split("\n");
        }
    }


    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        System.out.println("dafdafda");
        EnvVars envs = run.getEnvironment(listener);
        JOBNAME = envs.get("JOB_NAME");
        BUILDNUMBER = run.getNumber();
        String homePath = workspace.absolutize().getParent().getParent().getRemote() + "/";
        String jobPath = homePath + "/jobs/" + JOBNAME + "/builds/" + BUILDNUMBER + "/";
        String workspacePath = homePath + "/" + "workspace" + "/" + JOBNAME + "/";

        LOG = listener.getLogger();
        LOG.println("-----------------------------------------------------------------");
        LOG.println("增量打包插件执行中...");
        LOG.println("mailto kangshu@ciic.com.cn");
        LOG.println("-----------------------------------------------------------------");
        LOG.println("");
        LOG.println("脚本文件格式\t" + shType);
        LOG.println("");
        LOG.println("打包文件保存目录\t" + jobPath);
        LOG.println("编译输出目录\t" + workspacePath);
        LOG.println("构建编号\t" + run.getNumber());
        LOG.println("预设的服务器上的 备份目录\t" + bkRoot);
        LOG.println("预设的服务器上的 应用部署目录\t" + prodRoot);
        LOG.println("预设的服务器上的 增量包解压目录\t" + packageRoot);
        LOG.println("");


        Document doc = null;
        try {
            doc = new SAXReader().read(new File(jobPath + "changelog.xml"));
        } catch (DocumentException e) {
            throw new IOException("读取changelog.xml出错");
        }
        try (PackageTools pk = new PackageTools(jobPath, workspacePath, bkRoot + "/" + BUILDNUMBER + "/", prodRoot, packageRoot, shType)) {
            List<Node> nodes = doc.selectNodes("//path");
            for (Node n : nodes) {
                Element e = (Element) n;
                String action = e.attribute("action").getValue();
                String file = e.attribute("localPath").getValue();
                String kind = e.attribute("kind").getValue();
                pk.append(file, kind, action);
            }
            pk.save();
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckSrcPath(@QueryParameter String srcPath) {
            if (isNull(srcPath))
                return FormValidation.error("必须输入java src路径");
            return FormValidation.ok();
        }

        public FormValidation doCheckCompileTo(@QueryParameter String compileTo) {
            if (isNull(compileTo)) return FormValidation.error("必须输入java 编译输出路径");
            return FormValidation.ok();
        }

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
//            int regCount = 0 ,replaceCount = 0;
//            if (!isNull(regexStrs)) {
//                REGEXS = regexStrs.split("\n");
//                regCount = REGEXS.length;
//            }
//
//            if(regCount == 0 )
//            if (regCount != IncrementalBuilder.REPLACES.length)
//                return FormValidation.error("查找表达式与替换内容行数必须一致");
//
//
//            return FormValidation.ok();
//        }
//
//        public FormValidation doCheckReplaceStrs(@QueryParameter String replaceStrs) {
//            int regCount = 0, replaceCount = 0;
//
//            if (!isNull(replaceStrs))
//                replaceCount = replaceStrs.split("\n").length;
//            if (regCount != replaceCount)
//                return FormValidation.error("查找表达式与替换内容行数必须一致");
//
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

    public String getSrcPath() {
        return srcPath;
    }

    @DataBoundSetter
    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public String getCompileTo() {
        return compileTo;
    }

    @DataBoundSetter
    public void setCompileTo(String compileTo) {
        this.compileTo = compileTo;
    }

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
