package pub.timelyrain.jenkins.plugin.increment;

public class BaseTools {


    protected void log(String s) {
        IncrementalBuilder.LOG.println("\t" + s);
    }
}
