package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
public class Commit implements Serializable {
    /** Message. */
    private final String messages;
    /** date. */
    private final String dates;
    /** Path to parent file. */
    private File parents;
    /** Commit tree stored by this commit. */
    private TreeMap<String, String> commitedfiles;
    /** A mark used to find common ancestor. */
    private boolean isMarked;
    /** Second parent. */
    private File secondParent;

    public Commit(String message, File parent, String date) {
        this.messages = message;
        this.parents = parent;
        this.dates = date;
        this.commitedfiles = new TreeMap<String, String>();
        this.isMarked = false;
        this.secondParent = null;
    }

    public File getParentFile() {
        return this.parents;
    }
    public File getSecondParent() {
        return this.secondParent;
    }
    public void setSecondParent(File fileName) {
        this.secondParent = fileName;
    }
    public void clearMark() {
        this.isMarked = false;
    }
    public void mark() {
        this.isMarked = true;
    }
    public boolean isMarked() {
        return isMarked;
    }
    public Commit getParent() {
        return Utils.readObject(parents, Commit.class);
    }
    public String getMessage() {
        return messages;
    }
    public String getTimestamp() {
        String[] temp = dates.split(" ");
        String timestamp = "Date:" + " " + temp[0] + " " + temp[1] + " "
                + temp[2] + " " + temp[3] + " " + temp[5] + " -0800";
        return timestamp;
    }
    public TreeMap<String, String> getCommitedfiles() {
        return commitedfiles;
    }
}
