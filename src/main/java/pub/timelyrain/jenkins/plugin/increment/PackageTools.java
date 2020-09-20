package pub.timelyrain.jenkins.plugin.increment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageTools extends BaseTools implements AutoCloseable {


    private FileOutputStream bk = null;
    private FileOutputStream dp = null;
    private FileOutputStream rb = null;
    private Cmd cmd = null;
    String packageOutputPath;
    String bkRoot;
    String prodRoot;
    String distRoot;
    List<String> bkList, dpList, rbList;

    public PackageTools(String packageOutputPath, String workspacePath, String bkRoot, String prodRoot, String distRoot, String os) throws FileNotFoundException {
        this.packageOutputPath = packageOutputPath;
        this.bkRoot = bkRoot;
        this.prodRoot = prodRoot;
        this.distRoot = distRoot;

        cmd = new Cmd(os, bkRoot, prodRoot, distRoot);
        File packageDir = new File(packageOutputPath + "/package/");
        if (!packageDir.exists())
            packageDir.mkdirs();
        bk = new FileOutputStream(new File(packageOutputPath + "/package/01-bk." + cmd.extName()), false);
        dp = new FileOutputStream(new File(packageOutputPath + "/package/02-dp." + cmd.extName()), false);
        rb = new FileOutputStream(new File(packageOutputPath + "/package/03-rb." + cmd.extName()), false);

        bkList = new ArrayList<String>();
        dpList = new ArrayList<String>();
        rbList = new ArrayList<String>();

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
        log(""); //日志输出个空行，将一个文件分割为一组日志，方便查看。

        if ("dir".equalsIgnoreCase(kind) && "A".equalsIgnoreCase(action)) {
            //目录需要以/结尾
            file += "/";

            //新建目录 bk=null dp=创建目录 rb=删除目录
            bkList.add("");
            dpList.add(cmd.mkdir(file, null, prodRoot));
            rbList.add(cmd.rmdir(file, null, prodRoot));


        } else if ("dir".equalsIgnoreCase(kind) && "D".equalsIgnoreCase(action)) {
            //目录需要以/结尾
            file += "/";

            //删除目录 bk=复制目录下全部文件。dp=是删除目录 rb=从bk复制到原目录
            bkList.add(cmd.cpdir(file, prodRoot, bkRoot));
            dpList.add(cmd.rmdir(file, null, prodRoot));
            rbList.add(cmd.cpdir(file, bkRoot, prodRoot));

            archiveFile(file);
        } else if ("file".equalsIgnoreCase(kind) && "A".equalsIgnoreCase(action)) {
            //bk=无动 dp=复制文件 rb=删除发布目录文件
            bkList.add("");
            dpList.add(cmd.cp(file, distRoot, prodRoot));
            rbList.add(cmd.rm(file, null, prodRoot));
        } else if ("file".equalsIgnoreCase(kind) && "M".equalsIgnoreCase(action)) {
            //修改文件 bk=cp到备份  dp=复制文件 rb=从备份复制文件
            bkList.add(cmd.cp(file, prodRoot, bkRoot));
            dpList.add(cmd.cp(file, distRoot, prodRoot));
            rbList.add(cmd.cp(file, bkRoot, prodRoot));
        } else if ("file".equalsIgnoreCase(kind) && "D".equalsIgnoreCase(action)) {
            //删除文件 bk=cp到备份  dp=删除文件 rb=从备份复制文件
            bkList.add(cmd.cp(file, prodRoot, bkRoot));
            dpList.add(cmd.rm(file, null, prodRoot));
            rbList.add(cmd.cp(file, bkRoot, prodRoot));
        }

        log("备份命令\t" + bkList.get(bkList.size() - 1));
        log("部署命令\t" + bkList.get(bkList.size() - 1));
        log("回滚命令\t" + bkList.get(bkList.size() - 1));
        if ("file".equalsIgnoreCase(kind))
            archiveFile(file);
    }


    public void archiveFile(String file) throws IOException {
        //copy file to packageroot from distroot
        //拷贝文件
        FileUtils.copyFile(new File(distRoot + "/" + file), new File(packageOutputPath + "/package/" + file));
        log("拷贝文件\t" + packageOutputPath + "/package/" + file);
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

        String fileDir = file.substring(0, file.lastIndexOf("/"));
        String fileName = file.substring(file.lastIndexOf("/") + 1, file.length() - 6);
        Collection<File> files = FileUtils.listFiles(new File(distRoot + "/" + fileDir), new SurfixNameFileFilter(fileName + "$"), null);

        for (File f : files) {
            FileUtils.copyFile(f, new File(packageOutputPath + "/package/" + fileDir + f.getName()));
            log("拷贝文件\t" + packageOutputPath + "/package/" + fileDir + f.getName());
        }

    }

    public void close() {
        IOUtils.closeQuietly(bk);
        IOUtils.closeQuietly(dp);
        IOUtils.closeQuietly(rb);
    }

    public void save() throws IOException {
        //输出命令文件
        IOUtils.writeLines(bkList, cmd.cr(), bk, "utf-8");
        IOUtils.writeLines(dpList, cmd.cr(), dp, "utf-8");
        IOUtils.writeLines(rbList, cmd.cr(), rb, "utf-8");

        //压缩目录
        ZipTools.zipFiles(packageOutputPath + "/package/", packageOutputPath + "/package.zip");
        //删除package临时目录
        close();
        FileUtils.deleteDirectory(new File(packageOutputPath + "/package/"));
    }

    /**
     * 将java文件名调整为class
     *
     * @param file
     * @return
     */
    private String fileNameConvert(String file) {
        String retFile = file;
        if (retFile.endsWith(".java"))
            retFile = retFile.substring(0, retFile.length() - 5) + ".class";

        retFile = retFile.replaceAll("^src", "WebRoot/WEB-INF/classes");
        return retFile;
    }
}
