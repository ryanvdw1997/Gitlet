package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Represents the contents of a file.
 * @author Ryan Van de Water */
public class Blob implements Serializable {

    /** Constructor for a Blob object given a file.
     * @param f - the given file
     */
    public Blob(File f) {
        file = f;
        contents = gitlet.Utils.readContentsAsString(file);
        List<Object> sha1Passed = new ArrayList<>();
        sha1Passed.add(gitlet.Utils.serialize(contents));
        blobID = gitlet.Utils.sha1(sha1Passed);
    }

    /** Setter method for the contents instance variable.
     * @param newCont - the updated contents to be
     * written into the Blob.
     */
    void setContents(String newCont) {
        gitlet.Utils.writeContents(file, newCont);
        contents = newCont;
    }

    /** Getter method for the file instance variable.
     * @return the file variable
     */
    File getFile() {
        return file;
    }

    /** Getter method for the contents instance variable.
     * @return contents as they are in the Blob
     */
    String getContents() {
        return contents;
    }

    /** Getter method for the ID instance variable.
     * @return the ID for the Blob
     */
    String getID() {
        return blobID;
    }

    /** Denotes the file that the Blob represents. */
    private File file;

    /** Denotes the contents of the file the Blob represents. */
    private String contents;

    /** Denotes the sha-1 hash code specific to this blob. */
    private String blobID;
}
