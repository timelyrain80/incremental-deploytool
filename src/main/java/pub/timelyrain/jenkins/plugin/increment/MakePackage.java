package pub.timelyrain.jenkins.plugin.increment;

import org.apache.tools.ant.taskdefs.Pack;

public class MakePackage {
    public static void main(String[] args) throws Exception {
        if(args == null || args.length != 10){
            System.out.println("请输入参数");


        }

//        RuntimeSetting rs = new RuntimeSetting("d:/deploy/bk","d:/deploy/app","d:/deploy/package","linux",new String[]{".java", "src"}, new String[]{".class", "WebRoot/WEB-INF/classes"}, null, "D:/open-source/jenkins/data",6, "hrtest", System.out);
//        PackageTools pk = new PackageTools(rs);
//        pk.makePackage();

    }
}
