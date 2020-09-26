package pub.timelyrain.jenkins.plugin.increment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageTools {
    private Cmd cmd = null;
    RuntimeSetting rs;
    List<String> bkList = new ArrayList<>(), dpList = new ArrayList<>(), rbList = new ArrayList<>();

    public PackageTools(RuntimeSetting rs) {
        this.rs = rs;
        cmd = new Cmd(rs.getShType());
    }

    /**
     * 读取changelog 打包文件
     *
     * @throws IOException
     */
    public void makePackage() throws IOException {
        Document doc = null;
        try {
            doc = new SAXReader().read(new File(rs.getJobPath() + "changelog.xml"));
        } catch (DocumentException e) {
            throw new IOException("读取changelog.xml出错");
        }

        //读取changelist
        List<Node> nodes = doc.selectNodes("//path");
        if (nodes == null || nodes.isEmpty()) {
            log("\n无需要打包的文件，插件退出");
            return;
        }


        for (Node n : nodes) {
            Element e = (Element) n;
            String action = e.attribute("action").getValue();
            String file = e.attribute("localPath").getValue();
            String kind = e.attribute("kind").getValue();
            append(file, kind, action);
        }
        save();

    }


    /**
     * 传入新建目录 bk = null   dp = 创建目录 rb=删除目录
     * 传入删除目录 bk =复制目录下全部文件。dp=是删除目录 rb=从bk复制到原目录
     * 传入新建文件 bk=无动作    dp=复制文件 rb=删除发布目录文件
     * 传入修改文件 bk=cp到备份  dp=复制文件 rb=从备份复制文件
     * 传入删除文件 bk=cp到备份  dp=删除文件 rb=从备份复制文件
     *
     * @param file
     * @param kind   如果是目录类型 dir 创建目录 不创建回滚和备份。 如果类型是文件 ，创建回滚和备份脚本
     * @param action
     */
    public void append(String file, String kind, String action) throws IOException {
        file = fileNameConvert(file);
        log("\n处理 " + action + " " + file); //日志输出个空行，将一个文件分割为一组日志，方便查看。

        if ("dir".equalsIgnoreCase(kind) && "A".equalsIgnoreCase(action)) {
            //目录需要以/结尾
            file += "/";

            //新建目录 bk=null dp=创建目录 rb=删除目录
            bkList.add("");
            dpList.add(cmd.mkdir(file, null, cmd.getVar("PROD_ROOT")));
            rbList.add(0, cmd.rmdir(file, null, cmd.getVar("PROD_ROOT")));


        } else if ("dir".equalsIgnoreCase(kind) && "D".equalsIgnoreCase(action)) {
            //目录需要以/结尾
            file += "/";

            //删除目录 bk=复制目录下全部文件。dp=是删除目录 rb=从bk复制到原目录
            bkList.add(cmd.cpdir(file, cmd.getVar("PROD_ROOT"), cmd.getVar("BK_ROOT")));
            dpList.add(cmd.rmdir(file, null, cmd.getVar("PROD_ROOT")));
            rbList.add(0, cmd.cpdir(file, cmd.getVar("BK_ROOT"), cmd.getVar("PROD_ROOT")));

            archiveFile(file);
        } else if ("dir".equalsIgnoreCase(kind) && "M".equalsIgnoreCase(action)) {
            bkList.add("");
            dpList.add("");
            rbList.add("");
        } else if ("file".equalsIgnoreCase(kind) && "A".equalsIgnoreCase(action)) {
            //bk=无动 dp=复制文件 rb=删除发布目录文件
            bkList.add("");
            dpList.add(cmd.cp(file, cmd.getVar("PACKAGE_ROOT"), cmd.getVar("PROD_ROOT")));
            rbList.add(0, cmd.rm(file, null, cmd.getVar("PROD_ROOT")));
        } else if ("file".equalsIgnoreCase(kind) && "M".equalsIgnoreCase(action)) {
            //修改文件 bk=cp到备份  dp=复制文件 rb=从备份复制文件
            bkList.add(cmd.cp(file, cmd.getVar("PROD_ROOT"), cmd.getVar("BK_ROOT")));
            dpList.add(cmd.cp(file, cmd.getVar("PACKAGE_ROOT"), cmd.getVar("PROD_ROOT")));
            rbList.add(0, cmd.cp(file, cmd.getVar("BK_ROOT"), cmd.getVar("PROD_ROOT")));
        } else if ("file".equalsIgnoreCase(kind) && "D".equalsIgnoreCase(action)) {
            //删除文件 bk=cp到备份  dp=删除文件 rb=从备份复制文件
            bkList.add(cmd.cp(file, cmd.getVar("PROD_ROOT"), cmd.getVar("BK_ROOT")));
            dpList.add(cmd.rm(file, null, cmd.getVar("PROD_ROOT")));
            rbList.add(0, cmd.cp(file, cmd.getVar("BK_ROOT"), cmd.getVar("PROD_ROOT")));
        }

        log("备份命令\t" + bkList.get(bkList.size() - 1));
        log("部署命令\t" + dpList.get(dpList.size() - 1));
        log("回滚命令\t" + rbList.get(rbList.size() - 1));
        if ("file".equalsIgnoreCase(kind) && !"d".equalsIgnoreCase(action))
            archiveFile(file);
    }


    public void archiveFile(String file) throws IOException {
        //copy file to packageroot from distroot
        //拷贝文件
        FileUtils.copyFile(new File(rs.getWorkspacePath() + file), new File(rs.getJobPath() + "package/" + file));
        log("拷贝文件\t" + rs.getJobPath() + "package/" + file);
        archiveAnonymousFile(file);
    }

    /**
     * 如果是class文件，存在需要拷贝内部类的情况
     *
     * @param file
     * @throws IOException
     */
    public void archiveAnonymousFile(String file) throws IOException {
        if (file == null || !file.endsWith(".class"))
            return;

        String fileDir = file.substring(0, file.lastIndexOf(File.separator) + 1);
        String fileName = file.substring(file.lastIndexOf(File.separator) + 1, file.length() - 6);
        Collection<File> files = FileUtils.listFiles(new File(rs.getWorkspacePath() + fileDir), new SurfixNameFileFilter(fileName + "$"), null);

        for (File f : files) {
            FileUtils.copyFile(f, new File(rs.getJobPath() + "package/" + fileDir + f.getName()));
            log("拷贝文件\t" + rs.getJobPath() + "package/" + fileDir + f.getName());
        }

    }


    public void save() throws IOException {
        //输出命令文件
        try (
                FileOutputStream bk = new FileOutputStream(new File(rs.getJobPath() + "package/01-bk." + cmd.extName()), false);
                FileOutputStream dp = new FileOutputStream(new File(rs.getJobPath() + "package/02-dp." + cmd.extName()), false);
                FileOutputStream rb = new FileOutputStream(new File(rs.getJobPath() + "package/03-rb." + cmd.extName()), false);
        ) {
            //插入环境变量
            String env = cmd.setVar("BK_ROOT", rs.getBkRoot()) + "\n";
            env += cmd.setVar("PROD_ROOT", rs.getProdRoot()) + "\n";
            env += cmd.setVar("PACKAGE_ROOT", rs.getPackageRoot()) + "\n";

            bkList.add(0, env);
            dpList.add(0, env);
            rbList.add(0, env);

            IOUtils.writeLines(bkList, cmd.cr(), bk, cmd.encoding());
            IOUtils.writeLines(dpList, cmd.cr(), dp, cmd.encoding());
            IOUtils.writeLines(rbList, cmd.cr(), rb, cmd.encoding());
        }
        //压缩目录
        ZipTools.zipFiles(rs.getJobPath() + "package/", rs.getJobPath() + "package.zip");
        //删除package临时目录
        FileUtils.deleteDirectory(new File(rs.getJobPath() + "package/"));

    }

    /**
     * 将java文件名调整为class
     *
     * @param file
     * @return
     */
    private String fileNameConvert(String file) {
        String retFile = file;

        if (rs.getRegexList() == null || rs.getRegexList().length == 0)
            return file;

        if (rs.getReplaceList() == null || rs.getReplaceList().length == 0) {
            log("WARN\t查找表达式和替换内容不匹配，程序终止");
            throw new RuntimeException("查找表达式和替换内容不匹配，程序终止");
        }
        for (int i = 0; i < rs.getRegexList().length; i++) {
            retFile = retFile.replaceAll(rs.getRegexList()[i], rs.getReplaceList()[i]);
        }

        return retFile;
    }

    protected void log(String s) {
        rs.getLog().println("\t" + s);
    }
}
