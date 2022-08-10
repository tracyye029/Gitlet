package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

/** Gitlet class.
 * @author yuxinye
 */
public class Gitlet {
    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Gitlet folder. */
    static final File GITLET_DIC = Utils.join(CWD, ".gitlet");
    /** Blob folder. */
    static final File BLOB_FOLDER = Utils.join(GITLET_DIC, "blobs");
    /** Commits folder. */
    static final File COMMITS_FOLDER = Utils.join(GITLET_DIC, "commits");
    /** Staging area folder. */
    static final File STAGING_FOLDER = Utils.join(GITLET_DIC, "staging");
    /** Branch folder. */
    static final File BRANCH_FOLDER = Utils.join(GITLET_DIC, "branches");
    /** HeadBranch folder. */
    static final File HEADBRANCH_FOLDER = Utils.join(GITLET_DIC, "headbranch");
    /** Remote class folder. */
    static final File REMOTE_FOLDER = Utils.join(GITLET_DIC, "remotes");

    /** Initializes Git. Make the first commit. */
    public void init() {
        if (GITLET_DIC.exists()) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIC.mkdirs();
            BLOB_FOLDER.mkdirs();
            COMMITS_FOLDER.mkdirs();
            STAGING_FOLDER.mkdirs();
            BRANCH_FOLDER.mkdirs();
            HEADBRANCH_FOLDER.mkdirs();
            REMOTE_FOLDER.mkdirs();

            Commit initial = new Commit("initial commit",
                    null, null, new LinkedHashMap<>());
            File c = Utils.join(COMMITS_FOLDER, initial.getMySHA() + ".txt");
            if (!c.exists()) {
                try {
                    c.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeObject(c, initial);

            Branch head = new Branch("master", initial);
            File b = Utils.join(BRANCH_FOLDER,  "master.txt");
            if (!b.exists()) {
                try {
                    b.createNewFile();
                } catch (IOException p) {
                    p.printStackTrace();
                }
            }
            Utils.writeObject(b, head);
            File he = Utils.join(HEADBRANCH_FOLDER, "head.txt");
            if (!he.exists()) {
                try {
                    he.createNewFile();
                } catch (IOException p) {
                    p.printStackTrace();
                }
            }
            Utils.writeObject(he, head);

            StagingArea staging = new StagingArea();
            File s = Utils.join(STAGING_FOLDER, "stagingArea.txt");
            if (!s.exists()) {
                try {
                    s.createNewFile();
                } catch (IOException exp) {
                    exp.printStackTrace();
                }
            }
            Utils.writeObject(s, staging);
        }
    }

    /** Add a copy of a file to the staging area.
     * @param file name of a file
     */
    public void add(String file) {
        StagingArea staging = pullStagingArea();
        File b = Utils.join(CWD, file);
        if (b.exists()) {
            byte[] contents = Utils.readContents(b);
            String blobID = Utils.sha1(contents);

            if (!getCurrentCommit().getBlobs().containsKey(file)
                    || (getCurrentCommit().getBlobs().containsKey(file)
                    && !getCurrentCommit().getBlobs().
                    get(file).equals(blobID))) {
                if (!staging.getRemovedFiles().isEmpty()) {
                    if (staging.getRemovedFiles().containsKey(file)) {
                        staging.getRemovedFiles().remove(file);
                    }
                }
                staging.addFiles(file, blobID);
                File temp = Utils.join(BLOB_FOLDER, blobID + ".txt");
                Utils.writeContents(temp, contents);
                storeStagingArea(staging);
            } else {
                if (!staging.getRemovedFiles().isEmpty()) {
                    if (staging.getRemovedFiles().containsKey(file)) {
                        staging.getRemovedFiles().remove(file);
                    }
                }
                storeStagingArea(staging);
            }
        } else {
            System.out.print("File does not exist.");
            System.exit(0);
        }
    }

    /** Creates a new commit object and adds it to commit hashmap.
     * @param message a log message with the commit
     */
    public void commit(String message) {
        StagingArea staging = pullStagingArea();

        if (staging.getAddedFiles().isEmpty()
                && staging.getRemovedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.equals("")) {
            System.out.print("Please enter a commit message.");
            System.exit(0);
        }

        Commit parent = getCurrentCommit();
        LinkedHashMap<String, String> childBlobs
                = new LinkedHashMap<>(parent.getBlobs());
        for (String k : staging.getAddedFiles().keySet()) {
            childBlobs.put(k, staging.getAddedFiles().get(k));
        }

        for (String k : staging.getRemovedFiles().keySet()) {
            childBlobs.remove(k);
        }

        Commit current;
        String[] trim = message.split(" ");
        if (trim.length == 4 && trim[0].equals("Merged")
                && trim[2].equals("into")) {
            String merge = trim[1];
            File m = Utils.join(BRANCH_FOLDER, merge + ".txt");
            Commit mergedParent = Utils.readObject(m, Branch.class).getCommit();

            current = new Commit(message, parent.getMySHA(),
                    mergedParent.getMySHA(), childBlobs);
            File c = Utils.join(COMMITS_FOLDER, current.getMySHA() + ".txt");
            Utils.writeObject(c, current);
        } else {
            current = new Commit(message, parent.getMySHA(), null, childBlobs);
            File c = Utils.join(COMMITS_FOLDER, current.getMySHA() + ".txt");
            Utils.writeObject(c, current);
        }

        Branch head = new Branch(getHeadBranchName(), current);
        File b = Utils.join(BRANCH_FOLDER, getHeadBranchName() + ".txt");
        Utils.writeObject(b, head);
        storeHeadBranch(head);

        storeStagingArea(new StagingArea());
    }

    /** Remove a file.
     * @param fileName name of the file
     */
    public void rm(String fileName) {
        StagingArea staging = pullStagingArea();
        Commit current = getCurrentCommit();

        if (current.getBlobs().containsKey(fileName)) {
            Utils.restrictedDelete(fileName);
            staging.addFilesToRemoval(fileName,
                    current.getBlobs().get(fileName));

            if (staging.getAddedFiles().containsKey(fileName)) {
                staging.getAddedFiles().remove(fileName);
            }
            storeStagingArea(staging);
        } else if (staging.getAddedFiles().containsKey(fileName)) {
            staging.getAddedFiles().remove(fileName);
            storeStagingArea(staging);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Prints out all the commits from most recent to initial commit. */
    public void log() {
        Commit current = getCurrentCommit();
        while (current != null) {
            System.out.println("===");
            System.out.print("commit " + current.getMySHA() + "\n");

            if (current.getMergedParentSHA() != null) {
                System.out.println("Merge: "
                        + current.getParentSHA().substring(0, 7)
                        + " " + current.getMergedParentSHA().substring(0, 7));
            }
            System.out.print("Date: " + current.getTime() + "\n");
            System.out.print(current.getMessage() + "\n");
            System.out.print("\n");
            if (current.getParentSHA() == null) {
                break;
            } else {
                File temp = Utils.join(COMMITS_FOLDER,
                        current.getParentSHA() + ".txt");
                current = Utils.readObject(temp, Commit.class);
            }
        }
    }

    /** Prints out commit id, date, and message for all commits. */
    public void globalLog() {
        for (String commitID : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            File c = Utils.join(COMMITS_FOLDER, commitID);
            Commit commit = Utils.readObject(c, Commit.class);
            System.out.println("===");
            System.out.print("commit " + commit.getMySHA() + "\n");
            System.out.print("Date: " + commit.getTime() + "\n");
            System.out.print(commit.getMessage() + "\n");
            System.out.print("\n");
        }
    }

    /** Prints out all commit ids that match the commit message.
     * @param message a log message
     */
    public void find(String message) {
        boolean found = false;
        for (String commitID : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            File c = Utils.join(COMMITS_FOLDER, commitID);
            Commit commit = Utils.readObject(c, Commit.class);
            if (commit.getMessage().equals(message)) {
                found = true;
                System.out.println(commit.getMySHA());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Displays current existing branches, display all files. */
    public void status() {
        StagingArea staging = pullStagingArea();
        System.out.println("=== Branches ===");
        System.out.println("*" + getHeadBranchName());
        ArrayList<String> branches = new ArrayList<>();
        for (String branch : Utils.plainFilenamesIn(BRANCH_FOLDER)) {
            if (branch.equals(getHeadBranchName() + ".txt")) {
                continue;
            }
            branches.add(branch);
        }
        for (String branch : branches) {
            System.out.println(branch.substring(0, branch.length() - 4));
        }
        System.out.print("\n");

        System.out.println("=== Staged Files ===");
        Set<String> addedKeys = staging.getAddedFiles().keySet();
        ArrayList<String> stagedFiles = new ArrayList<>(addedKeys);
        Collections.sort(stagedFiles);
        for (String staged : stagedFiles) {
            System.out.println(staged);
        }
        System.out.print("\n");

        System.out.println("=== Removed Files ===");
        Set<String> removedKeys = staging.getRemovedFiles().keySet();
        ArrayList<String> removedFiles = new ArrayList<>(removedKeys);
        Collections.sort(removedFiles);
        for (String removed : removedFiles) {
            System.out.println(removed);
        }
        System.out.print("\n");

        statusEC1(staging);
        statusEC2(staging);
    }

    /** Helper status method: Modifications Not Staged For Commit.
     * @param staging current staging area
     */
    private void statusEC1(StagingArea staging) {
        System.out.println("=== Modifications Not Staged For Commit ===");
        ArrayList<String> modifiedNotTracked = new ArrayList<>();
        Commit current = getCurrentCommit();
        for (String fileName : current.getBlobs().keySet()) {
            File w = Utils.join(CWD, fileName);
            if (w.exists()) {
                byte[] contents = Utils.readContents(w);
                String blobID = Utils.sha1(contents);
                if (!blobID.equals(current.getBlobs().get(fileName))) {
                    if (staging.getAddedFiles().isEmpty()) {
                        modifiedNotTracked.add(fileName + " (modified)");
                    } else if (!staging.getAddedFiles().containsKey(fileName)) {
                        modifiedNotTracked.add(fileName + " (modified)");
                    }
                }
            } else {
                if (!staging.getRemovedFiles().containsKey(fileName)) {
                    modifiedNotTracked.add(fileName + " (deleted)");
                }
            }
        }

        if (!staging.getAddedFiles().isEmpty()) {
            for (String staged : staging.getAddedFiles().keySet()) {
                File w = Utils.join(CWD, staged);
                if (w.exists()) {
                    byte[] contents = Utils.readContents(w);
                    String blobID = Utils.sha1(contents);
                    if (!blobID.equals(staging.getAddedFiles().get(staged))) {
                        modifiedNotTracked.add(staged + " (modified)");
                    }
                } else {
                    modifiedNotTracked.add(staged + " (modified)");
                }
            }
        }
        Collections.sort(modifiedNotTracked);
        for (String m : modifiedNotTracked) {
            System.out.println(m);
        }
        System.out.print("\n");
    }

    /** Helper status method: Untracked files.
     * @param staging current staging area
     */
    private void statusEC2(StagingArea staging) {
        System.out.println("=== Untracked Files ===");
        ArrayList<String> untracked = new ArrayList<>();
        Set<String> currentFileNames = getCurrentCommit().getBlobs().keySet();
        ArrayList<String> committed = new ArrayList<>(currentFileNames);
        for (String file : Utils.plainFilenamesIn(CWD)) {
            if (!staging.getAddedFiles().containsKey(file)
                    && !committed.contains(file)) {
                untracked.add(file);
            } else if (committed.contains(file)
                    && staging.getRemovedFiles().containsKey(file)) {
                untracked.add(file);
            }
        }
        for (String u : untracked) {
            System.out.println(u);
        }
        System.out.print("\n");
    }

    /** Check out file in the current commit.
     * @param fileName name of the file.
     */
    private void checkOutFile(String fileName) {
        Commit headCommit = getCurrentCommit();
        if (!headCommit.getBlobs().keySet().contains(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            File f = Utils.join(CWD, fileName);
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File b = Utils.join(BLOB_FOLDER,
                    headCommit.getBlobs().get(fileName) + ".txt");
            byte[] contents = Utils.readContents(b);
            Utils.writeContents(f, contents);
        }
    }

    /** Check out the file in the given commit.
     * @param commitID commit ID
     * @param fileName name of the file
     */
    private void checkOutCommit(String commitID, String fileName) {
        int commitIDLength = commitID.length();
        boolean match = false;
        for (String name : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            if (commitID.equals(name.substring(0, name.length() - 4).
                    substring(0, commitIDLength))) {
                File mc = Utils.join(COMMITS_FOLDER, name);
                Commit matchedCommit = Utils.readObject(mc, Commit.class);
                if (!matchedCommit.getBlobs().keySet().contains(fileName)) {
                    System.out.println("File does not exist in that commit.");
                    System.exit(0);
                } else {
                    match = true;
                    File l = Utils.join(CWD, fileName);
                    if (!l.exists()) {
                        try {
                            l.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    File checkoutContents = Utils.join(BLOB_FOLDER,
                            matchedCommit.getBlobs().get(fileName) + ".txt");
                    byte[] contents = Utils.readContents(checkoutContents);
                    Utils.writeContents(l, contents);
                    break;
                }
            }
        }
        if (!match) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    /** Check out the given branch.
     * @param branchName given branch name
     */
    private void checkOutBranch(String branchName) {
        File b;
        if (branchName.contains("/")) {
            String[] arr = branchName.split("/");
            b = Utils.join(Utils.join(BRANCH_FOLDER, arr[0]), arr[1] + ".txt");
        } else {
            b = Utils.join(BRANCH_FOLDER, branchName + ".txt");
        }
        if (!b.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(getHeadBranchName())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit commitToRestore = Utils.readObject(b, Branch.class).getCommit();
        Commit current = getCurrentCommit();
        for (String fileName: commitToRestore.getBlobs().keySet()) {
            File f = Utils.join(CWD, fileName);
            if (!current.getBlobs().containsKey(fileName)) {
                if (f.exists()) {
                    byte[] co = Utils.readContents(f);
                    String blobID = Utils.sha1(co);
                    if (!blobID.equals(commitToRestore.
                            getBlobs().get(fileName))) {
                        System.out.println("There is an untracked "
                                + "file in the way; "
                                + "delete it, or add and commit it first.");
                        System.exit(0);
                    }
                } else {
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        for (String fileName: current.getBlobs().keySet()) {
            if (!commitToRestore.getBlobs().containsKey(fileName)) {
                File f = Utils.join(CWD, fileName);
                if (f.exists()) {
                    Utils.restrictedDelete(f);
                }
            }
        }
        for (String fileName : commitToRestore.getBlobs().keySet()) {
            File f = Utils.join(CWD, fileName);
            File checkoutContents = Utils.join(BLOB_FOLDER,
                    commitToRestore.getBlobs().
                            get(fileName) + ".txt");
            byte[] contents = Utils.readContents(checkoutContents);
            Utils.writeContents(f, contents);
        }
        storeStagingArea(new StagingArea());
        Branch branch = Utils.readObject(b, Branch.class);
        storeHeadBranch(branch);
    }


    /** Restore the history version of a file or an entire branch.
     * @param args string groups
     */
    public void checkOut(String... args) {
        if (args.length == 3) {
            String fileName = args[2];
            checkOutFile(fileName);

        } else if (args.length == 4) {
            String commitID = args[1];
            String fileName = args[3];
            checkOutCommit(commitID, fileName);

        } else if (args.length == 2) {
            String branchName = args[1];
            checkOutBranch(branchName);

        }
    }

    /** Create a new branch.
     * @param branchName name of the given branch
     */
    public void branch(String branchName) {
        File br = Utils.join(BRANCH_FOLDER, branchName + ".txt");
        if (br.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Commit current = getCurrentCommit();
        Branch newBranch = new Branch(branchName, current);
        File b = Utils.join(BRANCH_FOLDER, branchName + ".txt");
        Utils.writeObject(b, newBranch);
    }

    /** Removes a branch from the branches.
     * @param branchName name of the given branch
     */
    public void rmBranch(String branchName) {
        File br = Utils.join(BRANCH_FOLDER, branchName + ".txt");
        if (!br.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (pullHeadBranch().getName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else {
            br.delete();
        }
    }


    /** Checkout all the files back to the given commit.
     * @param commitID commit ID
     */
    public void reset(String commitID) {
        boolean match = false;
        int commitIDLength = commitID.length();
        for (String c : Utils.plainFilenamesIn(COMMITS_FOLDER)) {
            if (commitID.equals(c.substring(0, commitIDLength))) {
                match = true;
            }
        }
        if (!match) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        File c = Utils.join(COMMITS_FOLDER, commitID + ".txt");
        Commit commitToReset = Utils.readObject(c, Commit.class);
        Commit current = getCurrentCommit();

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (commitToReset.getBlobs().containsKey(fileName)
                    && !current.getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked "
                        + "file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        for (String fileName: current.getBlobs().keySet()) {
            if (!commitToReset.getBlobs().containsKey(fileName)) {
                File f = Utils.join(CWD, fileName);
                if (f.exists()) {
                    Utils.restrictedDelete(f);
                }
            }
        }

        for (String fileName : commitToReset.getBlobs().keySet()) {
            if (!current.getBlobs().containsKey(fileName)) {
                File f = Utils.join(CWD, fileName);
                if (!f.exists()) {
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                File checkoutContents = Utils.join(BLOB_FOLDER,
                        commitToReset.getBlobs().get(fileName) + ".txt");
                byte[] contents = Utils.readContents(checkoutContents);
                Utils.writeContents(f, contents);
            }
        }

        storeStagingArea(new StagingArea());
        Branch head = new Branch(getHeadBranchName(), commitToReset);
        File b = Utils.join(BRANCH_FOLDER, getHeadBranchName() + ".txt");
        Utils.writeObject(b, head);
        storeHeadBranch(head);

    }

    /** Merge helper 1.
     * @param givenBranch name of the given branch
     * @param br branch file
     */
    private void mergeHelper1(String givenBranch, File br) {
        StagingArea staging = pullStagingArea();

        if (!staging.getAddedFiles().isEmpty()
                || !staging.getRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!br.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (givenBranch.equals(getHeadBranchName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /** Merge helper 2.
     * @param givenBranch name of the given branch
     * @param currentCommit current commit
     * @param givenBranchLastCommit given branch commit
     * @return the split point commit
     */
    private Commit mergeHelper2(String givenBranch,
                                Commit currentCommit,
                                Commit givenBranchLastCommit) {
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!currentCommit.getBlobs().containsKey((fileName))
                    && givenBranchLastCommit.
                    getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked "
                        + "file in the way; "
                        + "delete it, or add and commit it first. ");
                System.exit(0);
            }
        }
        Commit splitPoint = getSplitPoint(givenBranchLastCommit,
                distanceFromHead(currentCommit));

        if (givenBranchLastCommit.getMySHA().equals(splitPoint.getMySHA())) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            System.exit(0);
        }
        if (currentCommit.getMySHA().equals(splitPoint.getMySHA())) {
            checkOut("checkout", givenBranch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        return splitPoint;
    }

    /** Merge helper 3.
     * @param givenBranch given branch
     * @param currentCommit current commit
     * @param givenBranchLastCommit given branch commit
     * @param splitPoint split point commit
     * @param hasConflicts indicator of merge conflicts
     */
    private void mergeHelper3(String givenBranch, Commit currentCommit,
                              Commit givenBranchLastCommit, Commit splitPoint,
                              boolean hasConflicts) {
        for (String fileName : givenBranchLastCommit.getBlobs().keySet()) {
            if (!splitPoint.getBlobs().containsKey(fileName)
                    && !currentCommit.getBlobs().containsKey(fileName)) {
                checkOut("checkout",
                        givenBranchLastCommit.getMySHA(), "--", fileName);
                add(fileName);
            } else if (!splitPoint.getBlobs().containsKey(fileName)
                    && currentCommit.getBlobs().containsKey(fileName)) {
                if (!currentCommit.getBlobs().get(fileName).
                        equals(givenBranchLastCommit.getBlobs().
                                get(fileName))) {
                    mergeConflicts(fileName, currentCommit,
                            givenBranchLastCommit);
                    add(fileName);
                    hasConflicts = true;
                }
            }
        }

        commit("Merged " + givenBranch + " into " + getHeadBranchName() + ".");
        if (hasConflicts) {
            System.out.println("Encountered a merge conflict.");
            System.exit(0);
        }
    }

    /** Merge files from the current branch to the given branch.
     * @param givenBranch name of the given branch
     */
    public void merge(String givenBranch) {
        boolean hasConflicts = false;
        File br = Utils.join(BRANCH_FOLDER, givenBranch + ".txt");
        mergeHelper1(givenBranch, br);
        Commit currentCommit = getCurrentCommit();
        Commit givenBranchLastCommit = Utils.
                readObject(br, Branch.class).getCommit();
        Commit splitPoint = mergeHelper2(givenBranch,
                currentCommit, givenBranchLastCommit);

        for (String fileName : splitPoint.getBlobs().keySet()) {
            if (currentCommit.getBlobs().containsKey(fileName)
                    && givenBranchLastCommit.
                    getBlobs().containsKey(fileName)) {
                if (!splitPoint.getBlobs().get(fileName).
                        equals(currentCommit.getBlobs().get(fileName))
                        && !splitPoint.getBlobs().get(fileName).
                        equals(givenBranchLastCommit.getBlobs().get(fileName))
                        && !currentCommit.getBlobs().get(fileName).
                        equals(givenBranchLastCommit.
                                getBlobs().get(fileName))) {
                    mergeConflicts(fileName, currentCommit,
                            givenBranchLastCommit);
                    add(fileName);
                    hasConflicts = true;
                } else if (!splitPoint.getBlobs().get(fileName).
                        equals(givenBranchLastCommit.getBlobs().get(fileName))
                        && splitPoint.getBlobs().get(fileName).
                        equals(currentCommit.getBlobs().get(fileName))) {
                    checkOut("checkout",
                            givenBranchLastCommit.getMySHA(), "--", fileName);
                    add(fileName);
                }
            } else if (currentCommit.getBlobs().containsKey(fileName)) {
                if (splitPoint.getBlobs().get(fileName).
                        equals(currentCommit.getBlobs().
                                get(fileName))) {
                    rm(fileName);
                } else {
                    mergeConflicts(fileName, currentCommit,
                            givenBranchLastCommit);
                    add(fileName);
                    hasConflicts = true;
                }
            } else if (givenBranchLastCommit.getBlobs().containsKey(fileName)) {
                if (!splitPoint.getBlobs().get(fileName).
                        equals(givenBranchLastCommit.
                                getBlobs().get(fileName))) {
                    mergeConflicts(fileName, currentCommit,
                            givenBranchLastCommit);
                    add(fileName);
                    hasConflicts = true;
                }
            }
        }
        mergeHelper3(givenBranch, currentCommit,
                givenBranchLastCommit, splitPoint, hasConflicts);

    }

    /** replace the contents of the conflicted file.
     * @param fileName name of the file
     * @param currentCommit current commit
     * @param givenCommit given commit
     */
    private void mergeConflicts(String fileName,
                                Commit currentCommit, Commit givenCommit) {
        byte[] m1 = "<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8);
        byte[] m2 = "=======\n".getBytes(StandardCharsets.UTF_8);
        byte[] m3 =  ">>>>>>>\n".getBytes(StandardCharsets.UTF_8);
        File file = Utils.join(CWD, fileName);

        byte[] r1, r3;
        if (currentCommit.getBlobs().containsKey(fileName)) {
            byte[] currentContents = Utils.readContents(file);
            r1 = arrayCopy(m1, currentContents);
        } else {
            r1 = m1;
        }
        byte[] r2 = arrayCopy(r1, m2);
        if (givenCommit.getBlobs().containsKey(fileName)) {
            File g = Utils.join(BLOB_FOLDER,
                    givenCommit.getBlobs().get(fileName) + ".txt");
            byte[] givenContents = Utils.readContents(g);
            r3 = arrayCopy(r2, givenContents);
        } else {
            r3 = r2;
        }
        byte[] r4 = arrayCopy(r3, m3);
        Utils.writeContents(file, r4);
    }

    /** Concatenate two byte arrays.
     * @param a1 first byte array
     * @param a2 second byte array
     * @return concatenated byte array
     */
    private byte[] arrayCopy(byte[] a1, byte[] a2) {
        byte[] a3 = new byte[a1.length + a2.length];
        System.arraycopy(a1, 0, a3, 0, a1.length);
        System.arraycopy(a2, 0, a3, a1.length, a2.length);
        return a3;
    }

    /** Return all parents in
     *  the current branch with their distances from the head.
     *  @param current current commit
     *  @return all parents with their distances
     *  */
    public LinkedHashMap<String, Integer> distanceFromHead(Commit current) {
        Queue<Commit> search = new LinkedList<>();
        LinkedHashMap<String, Integer> parentsTracked = new LinkedHashMap<>();

        search.add(current);
        parentsTracked.put(current.getMySHA(), 0);

        while (!search.isEmpty()) {
            Commit headOfQueue = search.peek();
            int distance = parentsTracked.get(headOfQueue.getMySHA()) + 1;
            if (headOfQueue.getParentSHA() != null) {
                File p = Utils.join(COMMITS_FOLDER,
                        headOfQueue.getParentSHA() + ".txt");
                Commit parent = Utils.readObject(p, Commit.class);
                search.add(parent);
                if (parentsTracked.containsKey(parent.getMySHA())) {
                    if (distance <= parentsTracked.get(parent.getMySHA())) {
                        parentsTracked.put(parent.getMySHA(), distance);
                    }
                } else {
                    parentsTracked.put(parent.getMySHA(), distance);
                }

            }
            if (headOfQueue.getMergedParentSHA() != null) {
                File m = Utils.join(COMMITS_FOLDER,
                        headOfQueue.getMergedParentSHA() + ".txt");
                Commit mergedParent = Utils.readObject(m, Commit.class);
                search.add(mergedParent);
                if (parentsTracked.containsKey(mergedParent.getMySHA())) {
                    if (distance <= parentsTracked.
                            get(mergedParent.getMySHA())) {
                        parentsTracked.put(mergedParent.getMySHA(), distance);
                    }
                } else {
                    parentsTracked.put(mergedParent.getMySHA(), distance);
                }
            }
            search.poll();
        }
        return parentsTracked;
    }

    /** Find the splitPoint.
     * @param given given commit
     * @param parentsTracked all parents tracked
     * @return split point commit
     */
    private Commit getSplitPoint(Commit given,
                                 LinkedHashMap<String, Integer>
                                         parentsTracked) {
        Queue<Commit> ln = new LinkedList<>();
        ArrayList<String> minList = new ArrayList<>();

        ln.add(given);
        if (parentsTracked.containsKey(given.getMySHA())) {
            minList.add(given.getMySHA());
        }

        while (!ln.isEmpty()) {
            Commit headOfLn = ln.peek();
            if (headOfLn.getParentSHA() != null) {
                File p = Utils.join(COMMITS_FOLDER,
                        headOfLn.getParentSHA() + ".txt");
                Commit parent = Utils.readObject(p, Commit.class);
                ln.add(parent);
                if (parentsTracked.containsKey(parent.getMySHA())) {
                    if (!minList.isEmpty()) {
                        int a = parentsTracked.get(parent.getMySHA());
                        int b = parentsTracked.
                                get(minList.get(minList.size() - 1));
                        if (a <= b) {
                            minList.add(parent.getMySHA());
                        }
                    } else {
                        minList.add(parent.getMySHA());
                    }
                }
            }
            if (headOfLn.getMergedParentSHA() != null) {
                File m = Utils.join(COMMITS_FOLDER,
                        headOfLn.getMergedParentSHA() + ".txt");
                Commit mergedParent = Utils.readObject(m, Commit.class);
                ln.add(mergedParent);
                if (parentsTracked.containsKey(mergedParent.getMySHA())) {
                    if (!minList.isEmpty()) {
                        int a = parentsTracked.get(mergedParent.getMySHA());
                        int b = parentsTracked.
                                get(minList.get(minList.size() - 1));
                        if (a <= b) {
                            minList.add(mergedParent.getMySHA());
                        }
                    } else {
                        minList.add(mergedParent.getMySHA());
                    }
                }
            }
            ln.poll();
        }
        String splitPointID = minList.get(minList.size() - 1);
        File z = Utils.join(COMMITS_FOLDER, splitPointID + ".txt");
        return Utils.readObject(z, Commit.class);
    }


    /** Get the current commit.
     * @return current commit
     */
    private Commit getCurrentCommit() {
        File he = Utils.join(HEADBRANCH_FOLDER, "head.txt");
        Branch head = Utils.readObject(he, Branch.class);
        return head.getCommit();
    }

    /** Get the head branch name.
     * @return head branch name
     */
    private String getHeadBranchName() {
        File he = Utils.join(HEADBRANCH_FOLDER, "head.txt");
        Branch head = Utils.readObject(he, Branch.class);
        return head.getName();
    }

    /** Serialize the head branch.
     * @param head branch
     */
    private void storeHeadBranch(Branch head) {
        File he = Utils.join(HEADBRANCH_FOLDER, "head.txt");
        Utils.writeObject(he, head);
    }

    /** deserialize the head branch.
     * @return head branch
     */
    private Branch pullHeadBranch() {
        File he = Utils.join(HEADBRANCH_FOLDER, "head.txt");
        return Utils.readObject(he, Branch.class);
    }

    /** Serialize the staging area.
     * @param staging current staging area
     */
    private void storeStagingArea(StagingArea staging) {
        File s = Utils.join(STAGING_FOLDER, "stagingArea.txt");
        Utils.writeObject(s, staging);
    }

    /** deserialize the staging area.
     * @return current staging area
     */
    private StagingArea pullStagingArea() {
        File s = Utils.join(STAGING_FOLDER, "stagingArea.txt");
        return Utils.readObject(s, StagingArea.class);
    }

    /** Add a remote to the local repo.
     * @param args input group strings
     */
    public void addRemote(String... args) {
        String remoteName = args[1];
        String remotePath = args[2];

        File remoteFile = Utils.join(REMOTE_FOLDER, remoteName + ".txt");
        if (remoteFile.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        } else {
            try {
                remoteFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Remote remote = new Remote(remoteName, remotePath);
            Utils.writeObject(remoteFile, remote);
        }
    }

    /** Remove the remote in the local repo.
     * @param remoteName name of the remote
     */
    public void rmRemote(String remoteName) {
        File remoteFile = Utils.join(REMOTE_FOLDER, remoteName + ".txt");
        if (!remoteFile.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        remoteFile.delete();
    }

    /** Push all the local commits and blobs to the remote branch.
     * @param remoteName name of remote
     * @param remoteBranchName name of the remote branch
     */
    public void push(String remoteName, String remoteBranchName) {
        File remoteFile = Utils.join(REMOTE_FOLDER, remoteName + ".txt");
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        Remote remote = Utils.readObject(remoteFile, Remote.class);
        File dic = new File(remote.getRemotePath());
        if (!dic.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }

        File rb = Utils.join(Utils.join(remote.getRemotePath(), "branches"),
                remoteBranchName + ".txt");
        if (!rb.exists()) {
            try {
                rb.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Commit remoteHeadCommit = Utils.readObject(rb, Branch.class).
                getCommit();
        Commit localHeadCommit = getCurrentCommit();
        if (!distanceFromHead(localHeadCommit).keySet().
                contains(remoteHeadCommit.getMySHA())) {
            System.out.println("Please pull down "
                    + "remote changes before pushing.");
            System.exit(0);
        }
        pushCommits(remote, localHeadCommit);

        Branch b = new Branch(remoteBranchName, localHeadCommit);
        Utils.writeObject(rb, b);

        File rhp = Utils.join(remote.getRemotePath(), "headbranch");
        File he = Utils.join(rhp, "head.txt");
        if (!he.exists()) {
            try {
                he.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(he, b);
    }

    /** Add or overwrite local parent blobs into the remote repo.
     * @param remote name of remote
     * @param parent parent or merged parent commit
     */
    private void pushBlobs(Remote remote, Commit parent) {
        for (String blobID : parent.getBlobs().values()) {
            File bl = Utils.join(BLOB_FOLDER, blobID + ".txt");
            byte[] contents = Utils.readContents(bl);
            File rbl = Utils.join(Utils.join(remote.getRemotePath(),
                            "blobs"),
                    blobID + ".txt");
            if (!rbl.exists()) {
                try {
                    rbl.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(rbl, contents);
        }
    }

    /** Add or overwrite the current commit and blobs into remote repo.
     * @param remote name of remote
     * @param localHeadCommit name of local head commit
     */
    private void pushCurrentCommitBlobs(Remote remote, Commit localHeadCommit) {
        File cmf = Utils.join(COMMITS_FOLDER,
                localHeadCommit.getMySHA() + ".txt");
        if (!cmf.exists()) {
            try {
                cmf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(cmf, localHeadCommit);

        for (String blobID : localHeadCommit.getBlobs().values()) {
            File bl = Utils.join(BLOB_FOLDER, blobID + ".txt");
            byte[] contents = Utils.readContents(bl);
            File rbl = Utils.join(Utils.join(remote.getRemotePath(), "blobs"),
                    blobID + ".txt");
            if (!rbl.exists()) {
                try {
                    rbl.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(rbl, contents);
        }
    }

    /** Add or overwrite local commits and blobs into the remote repo.
     * @param remote remote class
     * @param localHeadCommit local head commit
     */
    private void pushCommits(Remote remote, Commit localHeadCommit) {
        pushCurrentCommitBlobs(remote, localHeadCommit);

        Queue<Commit> search = new LinkedList<>();
        search.add(localHeadCommit);

        while (!search.isEmpty()) {
            Commit headOfQueue = search.peek();
            if (headOfQueue.getParentSHA() != null) {
                File pmf = Utils.join(COMMITS_FOLDER,
                        headOfQueue.getParentSHA() + ".txt");
                Commit parent = Utils.readObject(pmf, Commit.class);
                search.add(parent);
                File p = Utils.join(Utils.join(remote.getRemotePath(),
                                "commits"),
                        parent.getMySHA() + ".txt");
                if (!p.exists()) {
                    try {
                        p.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Utils.writeObject(p, parent);
                pushBlobs(remote, parent);
            }

            if (headOfQueue.getMergedParentSHA() != null) {
                File pmf = Utils.join(COMMITS_FOLDER,
                        headOfQueue.getMergedParentSHA() + ".txt");
                Commit mergedParent = Utils.readObject(pmf, Commit.class);
                search.add(mergedParent);
                File p = Utils.join(Utils.join(remote.getRemotePath(),
                                "commits"),
                        mergedParent.getMySHA() + ".txt");
                if (!p.exists()) {
                    try {
                        p.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Utils.writeObject(p, mergedParent);
                pushBlobs(remote, mergedParent);
            }
            search.poll();
        }
    }

    /** Add or overwrite all remote parent blobs into local repo.
     @param remote name of remote
     @param parent parent or merged parent commit
     */
    private void fetchBlobs(Remote remote, Commit parent) {
        for (String blobID : parent.getBlobs().values()) {
            File rbl = Utils.join(Utils.join(remote.getRemotePath(),
                            "blobs"),
                    blobID + ".txt");
            byte[] contents = Utils.readContents(rbl);
            File bl = Utils.join(BLOB_FOLDER, blobID + ".txt");
            if (!bl.exists()) {
                try {
                    bl.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(bl, contents);
        }
    }


    /** Add or overwrite the current remote commit and blobs into local repo.
     * @param remote name of remote
     * @param remoteCommit name of remote head commit
     */
    private void fetchCurrentCommitBlob(Remote remote, Commit remoteCommit) {
        File cmf = Utils.join(COMMITS_FOLDER, remoteCommit.getMySHA() + ".txt");
        if (!cmf.exists()) {
            try {
                cmf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(cmf, remoteCommit);

        for (String blobID : remoteCommit.getBlobs().values()) {
            File rbl = Utils.join(Utils.join(remote.getRemotePath(),
                            "blobs"),
                    blobID + ".txt");
            byte[] contents = Utils.readContents(rbl);
            File bl = Utils.join(BLOB_FOLDER, blobID + ".txt");
            if (!bl.exists()) {
                try {
                    bl.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(bl, contents);
        }
    }

    /** Add or overwrite all remote commits and blobs to the local repo.
     * @param remote remote class
     * @param remoteCommit remote commit
     */
    private void fetchCommits(Remote remote, Commit remoteCommit) {
        fetchCurrentCommitBlob(remote, remoteCommit);

        Queue<Commit> search = new LinkedList<>();
        search.add(remoteCommit);

        while (!search.isEmpty()) {
            Commit headOfQueue = search.peek();
            if (headOfQueue.getParentSHA() != null) {
                File p = Utils.join(Utils.join(remote.getRemotePath(),
                                "commits"),
                        headOfQueue.getParentSHA() + ".txt");
                Commit parent = Utils.readObject(p, Commit.class);
                search.add(parent);
                File pmf = Utils.join(COMMITS_FOLDER,
                        parent.getMySHA() + ".txt");
                if (!pmf.exists()) {
                    try {
                        pmf.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Utils.writeObject(pmf, parent);
                fetchBlobs(remote, parent);
            }
            if (headOfQueue.getMergedParentSHA() != null) {
                File m = Utils.join(Utils.join(remote.getRemotePath(),
                                "commits"),
                        headOfQueue.getMergedParentSHA() + ".txt");
                Commit mergedParent = Utils.readObject(m, Commit.class);
                search.add(mergedParent);
                File pmf = Utils.join(COMMITS_FOLDER,
                        mergedParent.getMySHA() + ".txt");
                if (!pmf.exists()) {
                    try {
                        pmf.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Utils.writeObject(pmf, mergedParent);
                fetchBlobs(remote, mergedParent);
            }
            search.poll();
        }
    }

    /** Fetch all remote commits and blobs to local repo,
     * but not merge it into the local master branch yet.
     * @param remoteName name of remote
     * @param remoteBranchName name of remote branch.
     */
    public void fetch(String remoteName, String remoteBranchName) {
        File remoteFile = Utils.join(REMOTE_FOLDER, remoteName + ".txt");
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        Remote remote = Utils.readObject(remoteFile, Remote.class);
        File dic = new File(remote.getRemotePath());
        if (!dic.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File rbp = Utils.join(remote.getRemotePath(), "branches");
        File remoteBranchFile = Utils.join(rbp, remoteBranchName + ".txt");
        if (!remoteBranchFile.exists()) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }

        File r = Utils.join(BRANCH_FOLDER, remoteName);
        if (!r.exists()) {
            r.mkdir();
        }
        File localRemoteBranch = Utils.join(r, remoteBranchName + ".txt");
        if (!localRemoteBranch.exists()) {
            try {
                localRemoteBranch.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Branch remoteBranch = Utils.readObject(remoteBranchFile, Branch.class);
        Commit remoteCommit = remoteBranch.getCommit();
        Branch b = new Branch(remoteName + "/"
                + remoteBranchName, remoteCommit);
        Utils.writeObject(localRemoteBranch, b);

        fetchCommits(remote, remoteCommit);
    }

    /** Fetch all remote commits and blobs to local repo
     * and merge it into the local master branch yet.
     * @param remoteName name of remote
     * @param remoteBranchName name of remote branch
     */
    public void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        File localRemoteBranch = Utils.join(Utils.join(BRANCH_FOLDER,
                        remoteName),
                remoteBranchName + ".txt");
        Branch l = Utils.readObject(localRemoteBranch, Branch.class);
        merge(l.getName());
    }

}
