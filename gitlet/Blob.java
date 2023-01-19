package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    /** pointedFile. */
    private File pointedfile;
    /** Blob content. */
    private String content;
    public Blob(File file) {
        this.pointedfile = file;
        this.content = Utils.readContentsAsString(pointedfile);
    }

    public String getContent() {
        return content;
    }

    public File getPointedFile() {
        return pointedfile;
    }
}
