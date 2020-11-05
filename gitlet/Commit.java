package gitlet;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Represents a Commit object or "snapshot" of files
 * at specific points in time.
 * @author Ryan Van de Water
 */
public class Commit implements Serializable {

    /** Initializes a commit with the given parameters as instance variables.
     * @param time - the time at which the commit was made
     * @param msg - client-specified message
     * @param track - a hashmap composing for file names and their
     *              associated blobs
     * @param father - the parent commit or commit that was
     *              made prior to this one
     */
    public Commit(String time, String msg, HashMap<String,
            Blob> track, Commit father) {
        timestamp = time;
        message = msg;
        tracking = track;
        parent = father;
        List<Object> sha1Passed = new ArrayList<>();
        sha1Passed.add(Utils.serialize(timestamp));
        sha1Passed.add(Utils.serialize(message));
        sha1Passed.add(Utils.serialize(tracking));
        sha1Passed.add(Utils.serialize(parent));
        commitID = Utils.sha1(sha1Passed);
    }

    /** Makes a shallow copy of the inputted commit.
     * @param c - the commit to be copied
     */
    public void copyFrom(Commit c) {
        timestamp = c.getTimeStamp();
        message = c.getMessage();
        tracking = c.getTracking();
        parent = c.getParent();
        commitID = c.getID();
    }

    /** Setter method for the ID of the Commit.
     * @param time - the date at which the Commit was made
     * @param msg - the message attached to the Commit
     * @param track - A HashMap of file names to their
     * respective Blob objects
     * @param par - the Commit that was made prior to this Commit
     */
    void setCommitID(String time, String msg,
                     HashMap<String, Blob> track, Commit par) {
        List<Object> sha1Passed = new ArrayList<>();
        sha1Passed.add(Utils.serialize(time));
        sha1Passed.add(Utils.serialize(msg));
        sha1Passed.add(Utils.serialize(track));
        sha1Passed.add(Utils.serialize(par));
        this.commitID = Utils.sha1(sha1Passed);
    }

    /** Allows you to update the Tracking instance variable with a new
     * file essentially.
     * @param str - The file name
     * @param blobby - The respective Blob object for the added file
     */
    void updateTracking(String str, Blob blobby) {
        if (tracking == null) {
            tracking = new HashMap<>();
            tracking.put(str, blobby);
        } else {
            if (!tracking.containsKey(str)) {
                tracking.put(str, blobby);
            } else {
                Blob beforeBlob = tracking.get(str);
                int diff = blobby.getContents().compareTo
                        (beforeBlob.getContents());
                if (diff != 0) {
                    tracking.put(str, blobby);
                }
            }
        }
    }

    /** Setter method for the timeStamp variable.
     * @param time - formatted date
     */
    void setTimestamp(String time) {
        this.timestamp = time;
    }

    /** Setter method for the message variable.
     * @param msg - Client-specific chosen string
     */
    void setMessage(String msg) {
        this.message = msg;
    }

    /** Setter method for the parent of a Commit.
     * @param c - Commit object
     */
    void setParent(Commit c) {
        this.parent = c;
    }

    /** Setter method for the tracking instance variable.
     * @param hashy - represents a file
     */
    void setTracking(HashMap<String, Blob> hashy) throws ClassCastException {
        if (hashy == null) {
            tracking = null;
        } else {
            HashMap<String, Blob> newInput;
            newInput = (HashMap<String, Blob>) hashy.clone();
            tracking = newInput;
        }
    }

    /** Getter method for the timeStamp instance variable.
     * @return formatted Date object
     */
    String getTimeStamp() {
        return timestamp;
    }

    /** Getter method for the message instance variable.
     * @return Client-specified string for the Commit object
     */
    String getMessage() {
        return message;
    }

    /** Getter method for the tracking instance variable.
     * @return HashMap(String, Blob) for the Commit object
     * which ultimately represents the files it is tracking
     */
    HashMap<String, Blob> getTracking() {
        return tracking;
    }

    /** Getter method for the parent instance variable.
     * @return prior commit made relative to this Commit object
     */
    Commit getParent() {
        return parent;
    }

    /** Getter method for the ID instance variable.
     * @return sha-1 hash code for the Commit object
     */
    String getID() {
        return commitID;
    }

    /** Denotes when the commit was made. */
    protected String timestamp;

    /** Denotes the client-specified message associated with the commit. */
    protected String message;

    /** Denotes the files that the commit is tracking changes of. */
    protected HashMap<String, Blob> tracking;

    /** Denotes the commit prior to this commit. */
    protected Commit parent;

    /** Denotes the sha-1 hash code specific to this commit. */
    protected String commitID;
}
