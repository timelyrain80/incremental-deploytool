package pub.timelyrain.jenkins.plugin.increment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTools {
    private static ZipOutputStream zipOutputStream;
    private static File file;
    private static String parentFileName;

    private static void zipFile(ZipOutputStream zipOutputStream, File file, String parentFileName) throws IOException {
        ZipTools.zipOutputStream = zipOutputStream;
        ZipTools.file = file;
        ZipTools.parentFileName = parentFileName;

        try (FileInputStream in = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(parentFileName);
            zipOutputStream.putNextEntry(zipEntry);
            int len;
            byte[] buf = new byte[8 * 1024];
            while ((len = in.read(buf)) != -1) {
                zipOutputStream.write(buf, 0, len);
            }
            zipOutputStream.closeEntry();
        }
    }

    /**
     * 递归压缩目录结构
     *
     * @param zipOutputStream
     * @param file
     * @param parentFileName
     */
    private static void directory(ZipOutputStream zipOutputStream, File file, String parentFileName) throws IOException {
        File[] files = file.listFiles();
        String parentFileNameTemp = null;
        for (File fileTemp : files) {
            parentFileNameTemp = parentFileName == null || "".equalsIgnoreCase(parentFileName) ? fileTemp.getName() : parentFileName + "/" + fileTemp.getName();
            if (fileTemp.isDirectory()) {
                directory(zipOutputStream, fileTemp, parentFileNameTemp);
            } else {
                zipFile(zipOutputStream, fileTemp, parentFileNameTemp);
            }
        }
    }

    /**
     * 压缩文件目录
     *
     * @param source  源文件目录（单个文件和多层目录）
     * @param distDir 目标目录
     */
    public static void zipFiles(String source, String distDir) throws IOException {
        File file = new File(source);
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(distDir);
                ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)
        ) {
            if (file.isDirectory()) {
                directory(zipOutputStream, file, "");
            } else {
                zipFile(zipOutputStream, file, "");
            }
        }
    }

}
