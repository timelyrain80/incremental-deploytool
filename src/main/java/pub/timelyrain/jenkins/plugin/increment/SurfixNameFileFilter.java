package pub.timelyrain.jenkins.plugin.increment;

import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;

public class SurfixNameFileFilter implements IOFileFilter {
    String surfix ;

    public SurfixNameFileFilter(String surfix) {
        this.surfix = surfix;
    }

    @Override
    public boolean accept(File file) {
        return file.getName().startsWith(surfix);
    }

    @Override
    public boolean accept(File dir, String name) {
        return false;
    }
}
