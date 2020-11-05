package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Represents a reference to commits and allows the
 * client to separate out different commits onto different references.
 * @author Ryan Van de Water
 */
public class Branch implements Serializable {

    /** Constructor for a Branch object given a client-specified
     * name and commit.
     * @param branchName - client-specified name
     * @param front - the commit at the head of this commit
     */
    public Branch(String branchName, Commit front) {
        head = front;
        name = branchName;
        List<Object> sha1Passed = new ArrayList<>();
        sha1Passed.add(Utils.serialize(head));
        sha1Passed.add(Utils.serialize(name));
        branchID = Utils.sha1(sha1Passed);
        tracked = new ArrayList<>();
        if (!(front.getTracking() == null)) {
            tracked.addAll(front.getTracking().keySet());
        }
    }

    /** Removes a file from the list of tracked files.
     * @param file - File to be removed
     */
    void removeFrom(String file) {
        tracked.remove(file);
    }

    /** Getter method for the list of tracked files in the branch.
     * @return the list of tracked files in the branch
     */
    List<String> getTracked() {
        return tracked;
    }

    /** Getter method for the sha-1 hash code specific to this branch.
     * @return the branch ID
     */
    String getID() {
        return branchID;
    }

    /** Getter method for the name of this branch.
     * @return the client-specified name of the branch
     */
    String getName() {
        return name;
    }

    /** Getter method for the Head commit of this branch.
     * @return the latest commit made in this branch
     */
    Commit getHEAD() {
        return head;
    }

    /** Setter method for the name of the branch.
     * @param newName - updated name
     */
    void setName(String newName) {
        name = newName;
    }

    /** Setter method for the Head commit of this object.
     * @param c - new Commit to be placed as the latest in the branch
     */
    void setHEAD(Commit c) {
        head = c;
    }

    /** Client-specified name of the branch object. */
    private String name;

    /** The latest commit made in this branch. */
    private Commit head;

    /** The sha-1 hash code specific to this branch. */
    private String branchID;

    /** The list of all files tracked by the head commit
     * of this branch object. */
    private List<String> tracked;
}
