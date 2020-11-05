package gitlet;


import java.io.File;

import static java.lang.System.exit;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;


/** Driver class for Gitlet, the tiny stupid
 * version-control system.
 *  @author Ryan Van de Water
 */
public class Main {

    /** Represents the CWD of the project. */
    static final File CURR = new File(".");

    /** Represents the .gitlet repository. */
    static final File GITLET_DIR = new File(".gitlet");

    /** Ensures a CWD environment. */
    public static void setUpPersistence() {
        CURR.mkdir();
    }
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            exit(0);
        }
        setUpPersistence();
        switch (args[0]) {
        case "init":
            checkgit(args[0]);
            init();
            break;
        case "add":
            add(args);
            break;
        case "commit":
            if (args.length == 1) {
                System.out.println("Please enter a commit message.");
                exit(0);
            }
            commit(args[1]);
            break;
        case "rm":
            rm(args[1]);
            break;
        case "log":
            log();
            break;
        case "global-log":
            File global = new File(GLOBAL_PATH);
            System.out.println(Utils.readContentsAsString(global));
            break;
        case "find":
            find(args[1]);
            break;
        case "status":
            status();
            break;
        case "checkout":
            checkout(args);
            break;
        case "branch":
            branch(args);
            break;
        case "rm-branch":
            rmbranch(args);
            break;
        case "reset":
            reset(args[1]);
            break;
        case "merge":
            merge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            exit(0);
        }
    }

    /** Initializes a .gitlet repository with all of the folders for
     * objects such as Commits, Branches, Logs, etc. to be placed into.
     */
    static void init() throws IOException {
        File addFolder = new File(ADD_PATH);
        addFolder.mkdir();
        File removalFolder = new File(REMOVAL_PATH);
        removalFolder.mkdir();
        Commit initialCommit = new Commit(FORMATTER.format(new Date(0)),
                "initial commit", null, null);
        File commitFolder = new File(COMMITS_PATH);
        commitFolder.mkdir();
        File newCommit = new File(pathMaker(COMMITS_PATH,
                String.format("%s", initialCommit.getID())));
        File globalLogFile = new File(GLOBAL_PATH);
        Branch master = new Branch("master", initialCommit);
        File logFile = new File(pathMaker(LOGS_PATH,
                String.format("%s.txt", master.getName())));
        File followFile = new File(FOLLOW_PATH);
        File notFollowFolder = new File(NOTFOLLOW_PATH);
        notFollowFolder.mkdir();
        File removedFolder = new File(REMOVED_PATH);
        removedFolder.mkdir();
        newCommit.createNewFile();
        globalLogFile.createNewFile();
        followFile.createNewFile();
        logFile.createNewFile();
        Utils.writeObject(newCommit, initialCommit);
        Utils.writeContents(logFile, String.format("===%ncommit %s%nDate: %s%n"
                        + "%s", initialCommit.getID(),
                initialCommit.getTimeStamp(), initialCommit.getMessage()));
        Utils.writeContents(globalLogFile, String.format("===%ncommit "
                        + "%s%nDate: %s%n%s", initialCommit.getID(),
                initialCommit.getTimeStamp(), initialCommit.getMessage()));
        Utils.writeObject(followFile, master);
    }

    /** Updates the staging area or Add folder in the index directory
     * with the specified file.
     * @param args - the specified file
     */
    static void add(String... args) throws IOException {
        checkgit(args[0]);
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        HashMap<String, Blob> track = currBranch.getHEAD().getTracking();
        for (String file : args) {
            if (file.matches("[a-z0-9]+\\.[a-z0-9]+")) {
                File remCheck = new File(REMOVAL_PATH, file);
                File currFile = new File(file);
                File indexed = new File(ADD_PATH, file);
                File remD = new File(REMOVED_PATH, file);
                if (remD.exists()) {
                    String cont = Utils.readContentsAsString(remD);
                    currFile.createNewFile();
                    Utils.writeContents(currFile, cont);
                    remD.delete();
                }
                if (currFile.exists()) {
                    String contents = Utils.readContentsAsString(currFile);
                    if (track != null && track.containsKey(file)) {
                        String trackCont = track.get(file).getContents();
                        if (trackCont.compareTo(contents) == 0) {
                            return;
                        }
                    }
                    if (remCheck.exists()) {
                        remCheck.delete();
                    }
                    indexed.createNewFile();
                    Utils.writeContents(indexed, contents);
                } else {
                    System.out.println("File does not exist.");
                }
            }
        }
    }

    /** Creates a commit object using the files that are currently in
     * the staging area.
     * @param args - the client-specified message accompanying the commit.
     */
    static void commit(String args) throws IOException {
        checkgit("commit");
        List<String> addList = Utils.plainFilenamesIn(new File(ADD_PATH));
        List<String> remList = Utils.plainFilenamesIn(new File(REMOVAL_PATH));
        File workingBranchFile = new File(FOLLOW_PATH);
        List<String> remDList = Utils.plainFilenamesIn(new File(REMOVED_PATH));
        Branch targetBranch = Utils.readObject(workingBranchFile, Branch.class);
        String currLog = pathMaker(LOGS_PATH, String.format("%s.txt",
                targetBranch.getName()));
        Commit currHead = new Commit(null, null, null, null);
        Commit newParent =  new Commit(null, null, null, null);
        newParent.copyFrom(targetBranch.getHEAD());
        currHead.setParent(newParent);
        currHead.setTracking(newParent.getTracking());
        if (addList.isEmpty() && remList.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        if (args.equals("")) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }
        for (String file : addList) {
            File addFile = new File(ADD_PATH, file);
            Blob blobby = new Blob(addFile);
            currHead.updateTracking(file, blobby);
            addFile.delete();
        }
        for (String file : remList) {
            currHead.getTracking().remove(file);
            File goner = new File(REMOVAL_PATH, file);
            goner.delete();
        }
        for (String file : remDList) {
            File remD = new File(REMOVED_PATH, file);
            remD.delete();
        }
        currHead.setTimestamp(FORMATTER.format(new Date()));
        currHead.setMessage(args);
        currHead.setCommitID(currHead.getTimeStamp(),
                currHead.getMessage(), currHead.getTracking(),
                currHead.getParent());
        Branch replacedBranch = new Branch(targetBranch.getName(),
                currHead);
        Utils.writeObject(workingBranchFile, replacedBranch);
        File logToBeChanged = new File(currLog);
        File globalToBeChanged = new File(GLOBAL_PATH);
        String globalBefore = Utils.readContentsAsString(globalToBeChanged);
        String logBefore = Utils.readContentsAsString(logToBeChanged);
        String newLog = logUpdate(logBefore, currHead);
        String newGlobal = globalUpdate(globalBefore, currHead);
        Utils.writeContents(logToBeChanged, newLog);
        Utils.writeContents(globalToBeChanged, newGlobal);
        File newCommitFile = new File(COMMITS_PATH, currHead.getID());
        newCommitFile.createNewFile();
        Utils.writeObject(newCommitFile, currHead);
    }

    /** Prints out the text file which as been tracking every commit and merge
     * inside the current branch.
     */
    static void log() {
        checkgit("log");
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        File currLog = new File(LOGS_PATH, String.format("%s.txt",
                currBranch.getName()));
        System.out.println(Utils.readContentsAsString(currLog));
    }

    /** Stages the specified file for removal.
     * @param args - the specified file name
     */
    static void rm(String args) throws IOException {
        checkgit("rm");
        Branch targetBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        Commit headComm = targetBranch.getHEAD();
        HashMap<String, Blob> tracking = headComm.getTracking();
        File checkAdd = new File(ADD_PATH, args);
        File remove = new File(REMOVAL_PATH, args);
        File currFile = new File(args);
        int removals = 0;
        if (checkAdd.exists()) {
            checkAdd.delete();
            removals++;
        }
        if (tracking != null && tracking.containsKey(args)) {
            currFile.delete();
            File removeAdd = new File(REMOVED_PATH, args);
            removeAdd.createNewFile();
            Utils.writeContents(removeAdd, tracking.get(args).getContents());
            tracking.remove(args);
            removals++;
        }
        remove.createNewFile();
        if (removals == 0) {
            System.out.println("No reason to remove the file.");
            exit(0);
        }
    }

    /** Has three orders of function. Checkout can be called on
     * a branch name, a commit ID and file name, or just a file name.
     * 1) Branch name - you change the .gitlet repository according to
     * the branch being checked out and make this branch the current branch.
     * 2) commit ID and file name - you find the file within the commit
     * with the specified ID and overwrite the file with the version of
     * the file in the given commit.
     * 3) File name - overwrites the file in the CWD with the
     * version of that filein the current Branch's head commit.
     * @param args - operands used for specifying which case is being applied.
     */
    static void checkout(String... args) throws IOException {
        checkgit(args[0]);
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        List<String> unFollows = Utils.plainFilenamesIn(
                new File(NOTFOLLOW_PATH));
        switch (args.length) {
        case 3:
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                exit(0);
            }
            HashMap<String, Blob> track = currBranch.getHEAD().getTracking();
            if (!track.containsKey(args[2])) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            Utils.writeContents(new File(args[2]),
                    track.get(args[2]).getContents());
            break;
        case 4:
            String commID = args[1];
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                exit(0);
            }
            if (findLong(commID) == null) {
                System.out.println("No commit with that id exists.");
                exit(0);
            } else {
                commID = findLong(commID);
            }
            checkoutFour(commID, args[3]);
            break;
        case 2:
            if (currBranch.getName().equals(args[1])) {
                System.out.println("No need to checkout the current branch.");
                exit(0);
            } else if (!unFollows.contains(args[1])) {
                System.out.println("No such branch exists.");
                exit(0);
            } else {
                checkoutBranch(currBranch, args[1]);
            }
            break;
        default:
            System.out.println("Command not recognized.");
            exit(0);
        }
    }


    /** Searches for the commit with the specified message.
     * @param message - the specified message
     */
    static void find(String message) {
        checkgit("find");
        int track = 0;
        List<String> allIDS = Utils.plainFilenamesIn(COMMITS_PATH);
        for (String id : allIDS) {
            Commit assessed = Utils.readObject(new File(COMMITS_PATH, id),
                    Commit.class);
            if (assessed.getMessage().equals(message)) {
                System.out.println(id);
                track++;
            }
        }
        if (track == 0) {
            System.out.println("Found no commit with that message.");
            exit(0);
        }
    }

    /** Shows the status of the .gitlet repository as it stands in the
     * current branch.
     */
    static void status() {
        checkgit("status");
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        File notFollow = new File(NOTFOLLOW_PATH);
        List<String> branchNames = Utils.plainFilenamesIn(notFollow);
        String first = String.format("=== Branches ===%n*%s",
                currBranch.getName());
        for (String branch : branchNames) {
            first = String.format("%s%n%s", first, branch);
        }
        File addFile = new File(ADD_PATH);
        List<String> addFiles = Utils.plainFilenamesIn(addFile);
        Collections.sort(addFiles);
        String second = "=== Staged Files ===";
        for (String file : addFiles) {
            second = String.format("%s%n%s", second, file);
        }
        List<String> remList = Utils.plainFilenamesIn(
                new File(REMOVED_PATH));
        String third = "=== Removed Files ===";
        for (String file : remList) {
            third = String.format("%s%n%s", third, file);
            File update = new File(REMOVED_PATH, file);
            update.delete();
        }
        String fourth = "=== Modifications Not Staged For Commit ===";
        String fifth = "=== Untracked Files ===";
        System.out.println(String.format("%s%n%n%s%n%n%s%n%n%s%n%n%s%n",
                first, second, third, fourth, fifth));
    }

    /** Creates a new branch with specified name and takes
     * on the head of the current branches head.
     * @param args - contains the specified name of the branch
     */
    static void branch(String... args) throws IOException {
        checkgit(args[0]);
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        List<String> notFollows = Utils.plainFilenamesIn(
                new File(NOTFOLLOW_PATH));
        String currLog = Utils.readContentsAsString(new File(LOGS_PATH,
                String.format("%s.txt", currBranch.getName())));
        if (args[1].equals(currBranch.getName())
                || notFollows.contains(args[1])) {
            System.out.println("A branch with that name already exists.");
            exit(0);
        } else {
            Commit newHead = new Commit(null, null, null, null);
            newHead.copyFrom(currBranch.getHEAD());
            Branch newBranch = new Branch(args[1], newHead);
            File branchFile = new File(NOTFOLLOW_PATH, newBranch.getName());
            File newLog = new File(LOGS_PATH, String.format("%s.txt",
                    newBranch.getName()));
            branchFile.createNewFile();
            newLog.createNewFile();
            Utils.writeObject(branchFile, newBranch);
            Utils.writeContents(newLog, currLog);
        }
    }

    /** Removes a branch from the .gitlet repository.
     * @param args - specifies which branch to remove
     */
    static void rmbranch(String... args) {
        checkgit(args[0]);
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        if (args[1].equals(currBranch.getName())) {
            System.out.println("Cannot remove the current branch.");
            exit(0);
        } else {
            List<String> notFollows = Utils.plainFilenamesIn(new
                    File(NOTFOLLOW_PATH));
            if (!notFollows.contains(args[1])) {
                System.out.println("A branch with that name does not exist.");
                exit(0);
            }
            File targetBranch = new File(NOTFOLLOW_PATH, args[1]);
            targetBranch.delete();
        }
    }

    /** Restores a branch to the commit with the specified commit ID.
     * In other words, changes the branches head from the head it has
     * now to the commit with the specified ID. Also updates the log
     * under this branch to get rid of commits that came after the
     * specified commit.
     * @param args - the ID of the commit to reset to.
     */
    static void reset(String args) throws IOException {
        checkgit("reset");
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        HashMap<String, Blob> tracked = currBranch.getHEAD().getTracking();
        if (findLong(args) == null) {
            System.out.println("No commit with that ID exists.");
            exit(0);
        } else {
            args = findLong(args);
        }
        if (!untrackedID(args)) {
            exit(0);
        }
        Commit targetComm = Utils.readObject(new File(COMMITS_PATH, args),
                Commit.class);
        if (targetComm.getTracking() != null) {
            for (String file : targetComm.getTracking().keySet()) {
                checkout("checkout", String.format("%s",
                        targetComm.getID()), "--", file);
            }
            for (String file : tracked.keySet()) {
                if (!(targetComm.getTracking().keySet().contains(file))) {
                    File goner = new File(file);
                    goner.delete();
                }
            }
        }
        Commit clone = cloneCommit(targetComm);
        String contents = "";
        while (clone != null) {
            contents = String.format("%s===%n%s %s%nDate: %s%n%s%n%n",
                    contents, "commit", clone.getID(), clone.getTimeStamp(),
                    clone.getMessage());
            clone = clone.getParent();
        }
        Utils.writeContents(new File(LOGS_PATH, String.format("%s.txt",
                currBranch.getName())), contents);
        currBranch.setHEAD(targetComm);
        clearStage();
        Utils.writeObject(new File(FOLLOW_PATH), currBranch);
    }

    /** Merges the files of the branch specified and the current branch
     * based on their shared split point Commit.
     * @param args - specified branch that is merging
     */
    static void merge(String... args) throws IOException {
        checkgit(args[0]);
        int mods = 0;
        Branch currB = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        List<String> addList = Utils.plainFilenamesIn(new File(ADD_PATH));
        List<String> remList = Utils.plainFilenamesIn(new File(REMOVAL_PATH));
        if (!addList.isEmpty() || !remList.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }
        if (currB.getName().equals(args[1])) {
            System.out.println("Cannot merge a branch with itself.");
            exit(0);
        } else if (!new File(NOTFOLLOW_PATH, args[1]).exists()) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }
        Branch given = Utils.readObject(new File(NOTFOLLOW_PATH, args[1]),
                Branch.class);
        HashMap<String, Blob> currT = currB.getHEAD().getTracking();
        HashMap<String, Blob> givenT = given.getHEAD().getTracking();
        Commit split = findSplit(currB, given);
        HashMap<String, Blob> splitT = split.getTracking();
        if (!checkingForUntracked(currB, given)) {
            exit(0);
        }
        if (split.getID().equals(given.getHEAD().getID())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            exit(0);
        } else if (split.getID().equals(currB.getHEAD().getID())) {
            checkout("%s %s", "checkout", given.getName());
            System.out.println("Current branch fast-forwarded.");
            exit(0);
        } else {
            List<String> allFiles = makeAll(currT, givenT, splitT);
            for (String file : allFiles) {
                if (splitT.containsKey(file)) {
                    mods += atSplit(currT, givenT, splitT, file, given);
                } else {
                    mods += notAtSplit(currT, givenT, file, args[1]);
                }
            }
        }
        String msg = String.format("Merged %s into %s.", given.getName(),
                            currB.getName());
        commit(msg);
        if (mods > 0) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Carries out appropriate procedures when the file being assessed
     * in a merge is at the split point.
     * @param currT - the files being tracked by the current branch
     * @param givenT - the files being tracked by the given branch
     * @param splitT - the files being tracked at the split point
     * @param file - the file being assessed
     * @param name - the given branch
     * @return a number denoting whether or not a merge conflict arose
     * @throws IOException
     */
    static int atSplit(HashMap<String, Blob> currT,
                       HashMap<String, Blob> givenT,
                       HashMap<String, Blob> splitT,
                       String file, Branch name) throws IOException {
        String splitCont = splitT.get(file).getContents();
        if (currT.containsKey(file) && givenT.containsKey(file)) {
            String currCont = currT.get(file).getContents();
            String giveCont = givenT.get(file).getContents();
            if (splitCont.compareTo(giveCont) != 0
                    && splitCont.compareTo(currCont) == 0) {
                checkout("checkout", name.getHEAD().getID(),
                        "--", file);
            } else if (splitCont.compareTo(giveCont) != 0
                    && splitCont.compareTo(currCont) != 0) {
                mergeConflict(file, currT, givenT);
                return 1;
            }
        } else if (currT.containsKey(file)) {
            String currCont = currT.get(file).getContents();
            if (splitCont.compareTo(currCont) == 0) {
                rm(file);
            } else {
                mergeConflict(file, currT, givenT);
                return 1;
            }
        } else {
            String givenCont = givenT.get(file).getContents();
            if (splitCont.compareTo(givenCont) != 0) {
                mergeConflict(file, currT, givenT);
                return 1;
            }
        }
        return 0;
    }

    /** Makes a list from all files being tracked by the a current
     * branch, a merged branch, their split point, and the files in the
     * CWD.
     * @param currT - files being tracked in the current branch
     * @param givenT - files being tracked in the given branch
     * @param splitT - files being tracked at the split point
     * @return list of all files being tracked and in CWD
     */
    static List<String> makeAll(HashMap<String, Blob> currT,
                             HashMap<String, Blob> givenT,
                                HashMap<String, Blob> splitT) {
        List<String> allFiles = new ArrayList<>();
        for (String file : currT.keySet()) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }
        for (String file : givenT.keySet()) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }
        for (String file : splitT.keySet()) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }
        for (String file : Utils.plainFilenamesIn(new File("."))) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }
        return allFiles;
    }

    /** Carries out appropriate operations when the file during a merge
     * is not at the split point.
     * @param currT - files being tracked in the current Branch
     * @param givenT - files being tracked in the given branch
     * @param file - the file being assessed
     * @param name - the name of the given branch
     * @return a number which will then be added to mods
     * @throws IOException
     */
    static int notAtSplit(HashMap<String, Blob> currT,
                       HashMap<String, Blob> givenT, String file,
                           String name)
            throws IOException {
        Branch currB = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        Branch given = Utils.readObject(new File(NOTFOLLOW_PATH, name),
                Branch.class);
        if (givenT.containsKey(file) && currT.containsKey(file)) {
            String currCont = currT.get(file).getContents();
            String giveCont = givenT.get(file).getContents();
            if (currCont.compareTo(giveCont) != 0) {
                System.out.println(file);
                mergeConflict(file, currT, givenT);
                return 1;
            }
        } else if (givenT.containsKey(file)
                && !currT.containsKey(file)) {
            branchSwap(currB, given);
            checkout("checkout", "--", file);
            File staged = new File(ADD_PATH, file);
            staged.createNewFile();
            branchSwap(given, currB);
            return 0;
        }
        return 0;
    }

    /** Updates the global log accordingly.
     * @param global - the previous global-log we are updating
     * @param curr - the current commit to which we are updating
     * the global-log with
     * @return the updated global-log text
     */
    static String globalUpdate(String global, Commit curr) {
        return String.format("===%ncommit %s%nDate: %s%n%s%n%n%s",
                curr.getID(), curr.getTimeStamp(), curr.getMessage(),
                global);
    }

    /** Updates the log of the current branch accordingly.
     * @param log - the previous log we are adding to
     * @param curr - the current commit to which we are updating
     * the log with
     * @return the updated log text
     */
    static String logUpdate(String log, Commit curr) {
        return String.format("===%ncommit %s%nDate: %s%n%s%n%n%s",
                curr.getID(), curr.getTimeStamp(), curr.getMessage(),
                log);
    }

    /** Carries out appropriate actions when checking out a commitID
     * and file.
     * @param commID - the commitID of the commit being investigated
     * @param fileName - the name of the file in the commit being
     * investigated
     */
    static void checkoutFour(String commID, String fileName)
            throws IOException {
        Commit targetComm = Utils.readObject(new File(COMMITS_PATH, commID),
                Commit.class);
        HashMap<String, Blob> targTrack = targetComm.getTracking();
        if (targTrack.containsKey(fileName)) {
            File currFile = new File(fileName);
            if (!currFile.exists()) {
                currFile.createNewFile();
            }
            Utils.writeContents(currFile,
                    targTrack.get(fileName).getContents());
        } else {
            System.out.println("File does not exist in that commit.");
            exit(0);
        }
    }

    /** Carries out appropriate operations when checking out a branch.
     * @param curr - current branch
     * @param name - the name of the branch being checked out
     */
    static void checkoutBranch(Branch curr, String name) throws IOException {
        HashMap<String, Blob> currTrack = curr.getHEAD().getTracking();
        Branch target = Utils.readObject(new File(NOTFOLLOW_PATH,
                    name), Branch.class);
        if (!checkingForUntracked(curr, target)) {
            exit(0);
        }
        HashMap<String, Blob> tracking = target.getHEAD().getTracking();
        if (tracking != null) {
            for (String file : tracking.keySet()) {
                File currFile = new File(file);
                if (!currFile.exists()) {
                    currFile.createNewFile();
                }
                Utils.writeContents(currFile, tracking.get(file).getContents());
            }
            for (String file : currTrack.keySet()) {
                if (!tracking.containsKey(file)) {
                    File goner = new File(file);
                    goner.delete();
                }
            }
        } else if (currTrack != null) {
            for (String file : currTrack.keySet()) {
                File goner = new File(file);
                goner.delete();
            }
        }
        branchSwap(curr, target);
        clearStage();
    }

    /** Used in the case of short ID data input.
     * Searches for the commit with the ID whos first
     * n (length of input id) characters matches the input
     * abbreviated ID.
     * @param id - the abbreviated ID
     * @return the associated 40-character sha1-hash of the
     * abbreviated ID.
     */
    static String findLong(String id) {
        List<String> commitList = Utils.plainFilenamesIn(
                new File(COMMITS_PATH));
        for (String file : commitList) {
            if (file.substring(0, id.length()).equals(id)) {
                return file;
            }
        }
        return null;
    }

    /** Checks to make sure the .gitlet directory is appropriate
     * for the given command.
     * @param command - any gitlet.Main command
     */
    static void checkgit(String command) {
        if (command.equals("init")) {
            if (new File(".gitlet").exists()) {
                System.out.println("A Gitlet version-control system already"
                        + " exists in the current directory.");
                exit(0);
            } else {
                GITLET_DIR.mkdir();
                File objects = new File(GITLET_DIR, "objects");
                objects.mkdir();
                File refs = new File(GITLET_DIR, "refs");
                refs.mkdir();
                File index = new File(GITLET_DIR, "index");
                index.mkdir();
                File logDir = new File(GITLET_DIR, "logs");
                logDir.mkdir();
            }
        } else {
            if (!new File(".gitlet").exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                exit(0);
            }
        }
    }

    /** Checks for files that could be overwritten in the case of a
     * checkout.
     * @param current - the current branch
     * @param checked - the branch being checked out
     * @return whether or not a file will be overwritten or not
     * as a result of this checkout
     */
    static Boolean checkingForUntracked(Branch current, Branch checked) {
        HashMap<String, Blob> currTrack = current.getHEAD().getTracking();
        HashMap<String, Blob> checkTrack = checked.getHEAD().getTracking();
        List<String> currFiles = Utils.plainFilenamesIn(new File("."));
        if (checkTrack == null) {
            return true;
        } else if (currTrack == null && !currFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return false;
        } else if (currTrack == null) {
            return true;
        } else {
            for (String file : currFiles) {
                if (!currTrack.containsKey(file)
                    && checkTrack.containsKey(file)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return false;
                }
            }
            return true;
        }
    }

    /** Checks for files that could be overwritten in the case of a
     * reset.
     * @param id - the commit ID that is being reset
     * @return whether or not a file will be overwritten or not
     * as a result of this reset
     */
    static Boolean untrackedID(String id) {
        Branch currBranch = Utils.readObject(new File(FOLLOW_PATH),
                Branch.class);
        Commit targetComm = Utils.readObject(new File(COMMITS_PATH, id),
                Commit.class);
        HashMap<String, Blob> checkTrack = targetComm.getTracking();
        HashMap<String, Blob> currTrack = currBranch.getHEAD().getTracking();
        List<String> currFiles = Utils.plainFilenamesIn(new File("."));
        if (checkTrack == null) {
            return true;
        } else if (currTrack == null && !currFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return false;
        } else if (currTrack == null) {
            return true;
        } else {
            for (String file : currFiles) {
                if (!currTrack.containsKey(file)
                        && checkTrack.containsKey(file)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return false;
                }
            }
            return true;
        }
    }

    /** Clears the staging area. */
    static void clearStage() {
        List<String> addList = Utils.plainFilenamesIn(
                new File(ADD_PATH));
        List<String> remList = Utils.plainFilenamesIn(
                new File(REMOVAL_PATH));
        for (String file : addList) {
            File old = new File(ADD_PATH, file);
            old.delete();
        }
        for (String file : remList) {
            File old = new File(REMOVAL_PATH, file);
            old.delete();
        }
    }

    /** Swaps the current branch with the specified branch.
     * @param current - current branch
     * @param replacer - branch to replace the current
     */
    static void branchSwap(Branch current, Branch replacer) throws IOException {
        File placed = new File(NOTFOLLOW_PATH, current.getName());
        placed.createNewFile();
        Utils.writeObject(new File(FOLLOW_PATH), replacer);
        Utils.writeObject(placed, current);
        File oldFile = new File(NOTFOLLOW_PATH, replacer.getName());
        oldFile.delete();
    }

    /** Finds the split point Commit of two branches.
     * @param current - current branch
     * @param given - branch to be merged with
     * @return the latest commit that these two branches have in common
     */
    static Commit findSplit(Branch current, Branch given) {
        HashMap<String, Blob> backup = new HashMap<>();
        Commit currHead = cloneCommit(current.getHEAD());
        while (currHead.getParent() != null) {
            Commit givenHead = cloneCommit(given.getHEAD());
            while (givenHead.getParent() != null) {
                if (currHead.getID().equals(givenHead.getID())) {
                    return currHead;
                }
                givenHead = cloneCommit(givenHead.getParent());
            }
            currHead = cloneCommit(currHead.getParent());
        }
        return new Commit(null, null, backup, null);
    }

    /** String concatenator to aid in making file paths with
     * File.separator objects in place.
     * @param currDir - the CWD
     * @param added - the strings that will be appended to the
     * CWD which ultimately makes up a file Path
     * @return a concatenated file Path string
     */
    static String pathMaker(String currDir, String... added) {
        for (String file : added) {
            currDir += File.separator + file;
        }
        return currDir;
    }

    /** Makes a shallow copy of the given commit.
     * @param c - the given commit
     * @return a copy of the given commit
     */
    static Commit cloneCommit(Commit c) {
        return new Commit(c.getTimeStamp(), c.getMessage(),
                c.getTracking(), c.getParent());
    }

    /** Fills the appropriate contents in the instance of a merge conflict.
     * @param file - name of the file being edited
     * @param curr - the files being tracked by the current branch's head
     * commit
     * @param given - the files being tracked by the given branch's head
     * commit
     * @throws IOException
     */
    static void mergeConflict(String file, HashMap<String, Blob> curr,
                          HashMap<String, Blob> given) throws IOException {
        File workFile = new File(file);
        if (!workFile.exists()) {
            workFile.createNewFile();
        }
        if (curr.containsKey(file) && given.containsKey(file)) {
            String currCont = curr.get(file).getContents();
            String giveCont = given.get(file).getContents();
            String input = String.format("<<<<<<< HEAD%n%s=======%n%s"
                    + ">>>>>>>%n", currCont, giveCont);
            Utils.writeContents(workFile, input);
        } else if (curr.containsKey(file)) {
            String currCont = curr.get(file).getContents();
            String input = String.format("<<<<<<< HEAD%n%s=======%n"
                    + ">>>>>>>%n", currCont);
            Utils.writeContents(workFile, input);
        } else {
            String giveCont = given.get(file).getContents();
            String input = String.format("<<<<<<<< HEAD%n=======%n%s"
                    + ">>>>>>>%n", giveCont);
            Utils.writeContents(workFile, input);
        }
    }

    /** The date format that is to be used in the log. */
    static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
            "EEE MMM dd HH:mm:ss yyyy Z");

    /** File path to the logs folder. */
    private static final String LOGS_PATH = ".gitlet" + File.separator
            + "logs";

    /** File path to the index folder. */
    private static final String INDEX_PATH = ".gitlet" + File.separator
            + "index";

    /** File path to the objects folder. */
    private static final String OBJECTS_PATH = ".gitlet" + File.separator
            + "objects";

    /** File path to the refs folder. */
    private static final String REFS_PATH = ".gitlet" + File.separator
            + "refs";

    /** File path to the commits folder. */
    private static final String COMMITS_PATH = pathMaker(OBJECTS_PATH,
            "commits");

    /** File path to the follow folder. */
    private static final String FOLLOW_PATH = pathMaker(REFS_PATH,
            "following");

    /** File path to the global-path folder. */
    private static final String GLOBAL_PATH = pathMaker(LOGS_PATH,
            "global.txt");

    /** File path to the branch folder containing the branches that
     * are not the current branch.
     */
    private static final String NOTFOLLOW_PATH = pathMaker(REFS_PATH,
            "not following");

    /** File path to the folder which holds the files that are staged for
     * addition.
     */
    private static final String ADD_PATH = pathMaker(INDEX_PATH,
            "add");

    /** File path to the folder which holds the files that are staged for
     * removal.
     */
    private static final String REMOVAL_PATH = pathMaker(INDEX_PATH,
            "removal");

    /** File path to the folder that holds the files which have
     * been most recently removed prior to a commit.
     */
    private static final String REMOVED_PATH = pathMaker(LOGS_PATH,
            "removed");
}
