package pub.timelyrain.jenkins.plugin.increment;

public class Cmd {
    public static final String WIN = "windows";
    public static final String LINUX = "linux";

    private boolean isWin;
    private String bkPath, prodPath, distPath;

    /**
     * 默认为Cmd.WIN  =  windows
     */
    public Cmd(String bkPath, String prodPath, String distPath) {
        isWin = true;
        this.bkPath = bkPath;
        this.prodPath = prodPath;
        this.distPath = distPath;
    }

    public Cmd(String os, String bkPath, String prodPath, String distPath) {
        this.isWin = WIN.equalsIgnoreCase(os);
        this.bkPath = bkPath;
        this.prodPath = prodPath;
        this.distPath = distPath;
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
            return path;
    }

    public String extName() {
        if (isWin)
            return "bat";
        else
            return "sh";
    }

    public String cr() {
        if (isWin)
            return "\r\n";
        else
            return "\n";
    }

    private String multiFile(String file) {
        int idx = file.lastIndexOf(".");
        return file.substring(0, idx) + "*" + file.substring(idx, file.length());
    }

    private String dstPath(String file) {
        int idx = file.lastIndexOf("/");
        return file.substring(0, idx) + "/";
    }
}
