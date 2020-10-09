package pub.timelyrain.jenkins.plugin.increment.pojo;

public class ChangeFile {
    private String file, kind, action;

    public ChangeFile(String file, String kind, String action) {
        this.file = file;
        this.kind = kind;
        this.action = action;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return action + "\t" + kind + "\t" + file;
    }
}
