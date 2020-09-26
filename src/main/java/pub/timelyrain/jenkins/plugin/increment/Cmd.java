package pub.timelyrain.jenkins.plugin.increment;

import java.io.File;

public class Cmd {
    public static final String WIN = "windows";
    public static final String LINUX = "linux";

    private boolean isWin;

    /**
     * 默认为Cmd.WIN  =  windows
     */
    public Cmd() {
        isWin = true;
    }


    public Cmd(String os) {
        this.isWin = WIN.equalsIgnoreCase(os);
    }


    public String setVar(String var, String value) {
        String sh = null;
        if (isWin)
            sh = "set " + var + "=" + pathConvert(value);
        else
            sh = "export " + var + "=" + pathConvert(value);
        return sh;
    }

    public String getVar(String var) {
        String sh = null;
        if (isWin)
            sh = "%" + var + "%";
        else
            sh = "$" + var;
        return sh;
    }

    public String cp(String file, String src, String dst) {
        String sh = null;
        if (isWin)
            sh = "xcopy /y ";
        else
            sh = "cp /f ";

        String param = src + "/" + multiFile(file) + " " + dst + "/" + dstPath(file);
        return sh + pathConvert(param);
    }

    public String cpdir(String path, String src, String dst) {
        String sh = null;
        if (isWin)
            sh = "xcopy /f /e ";
        else
            sh = "cp -r ";

        String param = src + "/" + path + " " + dst + "/" + path;
        return sh + pathConvert(param);
    }

    public String mkdir(String path, String src, String dst) {
        if (!path.endsWith("/"))
            path = path.substring(0, path.lastIndexOf("/"));

        String sh = null;
        if (isWin)
            sh = "mkdir ";
        else
            sh = "mkdir -p ";
        String param = dst + "/" + path;
        return sh + pathConvert(param);
    }

    public String rm(String file, String src, String dst) {
        String sh = null;
        if (isWin)
            sh = "del ";
        else
            sh = "rm ";
        String param = dst + "/" + file;
        return sh + pathConvert(param);
    }

    public String rmdir(String path, String src, String dst) {
        String sh = null;
        if (isWin)
            sh = "rd /s /q ";
        else
            sh = "rm -rf ";
        String param = dst + "/" + path;
        return sh + pathConvert(param);
    }

    public String pathConvert(String path) {
//        path += cr();
        if (isWin)
            return path.replaceAll("/", "\\\\");
        else
            return path.replaceAll("\\\\", "/");
    }

    public String extName() {
        if (isWin)
            return "bat";
        else
            return "sh";
    }

    public String separator() {
        if (isWin)
            return "\\";
        else
            return "/";
    }

    public String cr() {
        if (isWin)
            return "\r\n";
        else
            return "\n";
    }

    public String encoding() {
        if (isWin)
            return "GBK";
        else
            return "utf-8";
    }

    private String multiFile(String file) {
        int idx = file.lastIndexOf(".");
        return file.substring(0, idx) + "*" + file.substring(idx, file.length());
    }

    private String dstPath(String file) {
        int idx = file.lastIndexOf(File.separator);
        return file.substring(0, idx) + "/";
    }
}
