package gitlet;

import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;


public class GitletSystem {

    public GitletSystem() {
    }

    public void init() {
        TreeMap<String, String> branches = new TreeMap<String, String>();
        TreeMap staging = new TreeMap<String, String>();
        File cwd = new File(System.getProperty("user.dir"));
        Commit initial = new Commit("initial commit",
                null, "Thu Jan 1 00:00:00 1970");
        File gitlet = new File(".gitlet");
        if (gitlet.mkdir()) {
            String useless = "";
        } else {
            System.out.println(" A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        File commit0 = Utils.join(gitlet, Utils.sha1(Utils.serialize(initial)));
        try {
            commit0.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(commit0, initial);
        branches.put("master", Utils.sha1(Utils.serialize(initial)));
        File headpointer = Utils.join(gitlet, "head");
        try {
            headpointer.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(headpointer, "master");
        File stagingarea = Utils.join(gitlet, "staging");
        try {
            stagingarea.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(stagingarea, staging);

        File branchesfile = Utils.join(gitlet, "branches");
        try {
            branchesfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(branchesfile, branches);
        File removalstage = Utils.join(gitlet, "removal");
        try {
            removalstage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        TreeMap removaltree = new TreeMap<>();
        Utils.writeObject(removalstage, removaltree);
    }



    public void add(String filename) {
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches = Utils.readObject(
                Utils.join(".gitlet", "branches"), TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branches.get(headpointer);

        File cwd = new File(System.getProperty("user.dir"));
        File addedfile = Utils.join(cwd, filename);
        if (!addedfile.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String addedfilename = addedfile.getName();
        Blob addedblob = new Blob(addedfile);
        File newblob = Utils.join(".gitlet",
                Utils.sha1(Utils.serialize(addedblob)));

        Commit lastcommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);
        TreeMap<String, String> lastcommitfiles = lastcommit.getCommitedfiles();
        File stagingfile = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> staging = Utils.readObject
                (stagingfile, TreeMap.class);
        if (lastcommitfiles.containsKey(addedfilename)
                && lastcommitfiles.get(addedfilename).equals(
                        Utils.sha1(Utils.serialize(addedblob)))) {
            if (staging.containsKey(addedfilename)) {
                staging.remove(addedfilename);
            }
        }
        if (!lastcommitfiles.containsKey(addedfilename)
                || !lastcommitfiles.get(addedfilename).equals(
                        Utils.sha1(Utils.serialize(addedblob)))) {
            staging.put(addedfilename, Utils.sha1(Utils.serialize(addedblob)));
            try {
                newblob.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(newblob, addedblob);
        }

        Utils.writeObject(stagingfile, staging);
        File removal = Utils.join(".gitlet", "removal");
        TreeMap removaltree = Utils.readObject(removal, TreeMap.class);
        if (removaltree.containsKey(filename)) {
            removaltree.remove(filename);
        }
        Utils.writeObject(removal, removaltree);
    }

    public Commit commitHelper(TreeMap<String, String> removaltree,
                               TreeMap<String, String> lastcommitfiles,
                               String message,
                               TreeMap<String, String> branches,
                               Commit newcommit) {
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, String>> e1 = removaltree.entrySet();
        for (Map.Entry<String, String> e2 : e1) {
            if (lastcommitfiles.containsKey(e2.getKey())) {
                newcommit.getCommitedfiles().remove(e2.getKey());
            }
        }
        removaltree.clear();
        if (message.contains("Merged")) {
            String[] messagePieces = message.split(" ");
            String givenBranchName = messagePieces[1];
            String givenCommitID = branches.get(givenBranchName);
            File secondParentFile = Utils.join(".gitlet", givenCommitID);
            newcommit.setSecondParent(secondParentFile);
        }
        File newcommitfile = new File(".gitlet", Utils.sha1(
                Utils.serialize(newcommit)));
        try {
            newcommitfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newcommit;
    }

    public void commit(String message) {
        File stagingfile = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> staging =
                Utils.readObject(stagingfile, TreeMap.class);
        File removal = Utils.join(".gitlet", "removal");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> removaltree =
                Utils.readObject(removal, TreeMap.class);
        if (staging.isEmpty() && removaltree.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches = Utils.readObject(Utils.join(
                ".gitlet", "branches"), TreeMap.class);
        File headfile = new File(".gitlet" + File.separator + "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branches.get(headpointer);
        Commit lastcommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);
        Commit newcommit = new Commit(message, Utils.join(".gitlet",
                Utils.sha1(Utils.serialize(lastcommit))),
                new Date().toString());
        TreeMap<String, String> lastcommitfiles = lastcommit.getCommitedfiles();
        newcommit.getCommitedfiles().putAll(lastcommitfiles);
        Set<Map.Entry<String, String>> entries = staging.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (lastcommitfiles.containsKey(entry.getKey())) {
                File lastblobfile = Utils.join(".gitlet",
                        lastcommitfiles.get(entry.getKey()));
                Blob lastblob = Utils.readObject(lastblobfile, Blob.class);
                if (!Utils.sha1(Utils.serialize(lastblob)).equals(
                        entry.getValue())) {
                    newcommit.getCommitedfiles().replace(entry.getKey(),
                            entry.getValue());
                }
            } else {
                newcommit.getCommitedfiles().put(entry.getKey(),
                        entry.getValue());
            }
        }
        staging.clear();
        newcommit = commitHelper(removaltree, lastcommitfiles,
                message, branches, newcommit);
        File newcommitfile = new File(".gitlet", Utils.sha1(
                Utils.serialize(newcommit)));
        branches.put(headpointer, Utils.sha1(Utils.serialize(newcommit)));
        Utils.writeObject(Utils.join(".gitlet", "branches"),
                branches);
        Utils.writeObject(newcommitfile, newcommit);
        Utils.writeObject(stagingfile, staging);
        Utils.writeObject(removal, removaltree);
        branches.replace("master", newcommitfile.getName());
    }

    public void checkout2(String... args) {
        Commit thiscommit = null;
        File branchesfile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches = Utils.readObject(
                branchesfile, TreeMap.class);
        if (!branches.containsKey(args[1])) {
            System.out.println("No such branch exists.");
            return;
        }
        File headfile = Utils.join(".gitlet", "head");
        String head = Utils.readContentsAsString(headfile);
        File cwd = new File(System.getProperty("user.dir"));
        if (head.equals(args[1])) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String headcommitid = branches.get(head);
        Commit lastcommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);
        File staging = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingTree = Utils.readObject(
                staging, TreeMap.class);
        for (File each : cwd.listFiles()) {
            if (each.getName().equals(".gitlet")) {
                String useless = "";
            } else if (!lastcommit.getCommitedfiles().containsKey(
                    each.getName())
                    && !stagingTree.containsKey(each.getName())) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it,"
                        + " or add and commit it first.");
                return;
            }
        }
        checkout2Helper(branches, thiscommit, args, staging);
        Utils.writeContents(headfile, args[1]);
    }

    public void checkout2Helper(TreeMap<String, String> branches,
                                Commit thiscommit, String[] args,
                                File staging) {
        File cwd = new File(System.getProperty("user.dir"));
        String thiscommitid = branches.get(args[1]);
        thiscommit = Utils.readObject(
                Utils.join(".gitlet", thiscommitid), Commit.class);
        Set<Map.Entry<String, String>> entries =
                thiscommit.getCommitedfiles().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String blobsha1 = entry.getValue();
            Blob thisblob = Utils.readObject(
                    Utils.join(".gitlet", blobsha1), Blob.class);
            String content = thisblob.getContent();
            File workspacefile = Utils.join(cwd, entry.getKey());
            if (workspacefile.exists()) {
                Utils.writeContents(workspacefile, content);
            } else {
                File newworkspacefile = new File(cwd, entry.getKey());
                try {
                    newworkspacefile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Utils.writeContents(newworkspacefile, content);
            }
        }
        for (File each : cwd.listFiles()) {
            if (!thiscommit.getCommitedfiles().containsKey(each.getName())) {
                File deadfile = Utils.join(cwd, each.getName());
                deadfile.delete();
            }
        }
        TreeMap emptystagingtree = new TreeMap<>();
        Utils.writeObject(staging, emptystagingtree);
    }

    public void checkout3(String... args) {
        Commit thiscommit = null;
        File branchesfile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches = Utils.readObject(
                branchesfile, TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branches.get(headpointer);
        thiscommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);

        if (!thiscommit.getCommitedfiles().containsKey(args[2])) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobsha1 = thiscommit.getCommitedfiles().get(args[2]);
        Blob thisblob = Utils.readObject(Utils.join(
                ".gitlet", blobsha1), Blob.class);
        String content = thisblob.getContent();
        File cwd = new File(System.getProperty("user.dir"));
        File workspacefile = Utils.join(cwd, args[2]);
        if (workspacefile.exists()) {
            Utils.writeContents(workspacefile, content);
        } else {
            File newworkspacefile = new File(cwd, args[2]);
            try {
                newworkspacefile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(newworkspacefile, content);
        }
    }

    public void checkout(String... args) {
        Commit thiscommit = null;
        if (args.length == 2) {
            checkout2(args);

        } else if (args.length == 3) {
            checkout3(args);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            File gitlet = new File(".gitlet");
            for (File gitletfile: gitlet.listFiles()) {
                if (gitletfile.getName().equals(args[1])
                        || gitletfile.getName().startsWith(args[1])) {
                    thiscommit = Utils.readObject(gitletfile, Commit.class);
                    break;
                }
            }
            if (thiscommit == null) {
                System.out.println("No commit with that id exists.");
                return;
            }
            if (!thiscommit.getCommitedfiles().containsKey(args[3])) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String blobsha1 = thiscommit.getCommitedfiles().get(args[3]);
            Blob thisblob = Utils.readObject(
                    Utils.join(".gitlet", blobsha1), Blob.class);
            String content = thisblob.getContent();
            File cwd = new File(System.getProperty("user.dir"));
            File workspacefile = Utils.join(cwd, args[3]);
            if (workspacefile.exists()) {
                Utils.writeContents(workspacefile, content);
            } else {
                File newworkspacefile = new File(cwd, args[3]);
                try {
                    newworkspacefile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Utils.writeContents(newworkspacefile, content);
            }
        }
    }

    public void log() {
        File branchesfile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches =
                Utils.readObject(branchesfile, TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branches.get(headpointer);
        Commit thiscommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);
        while (!thiscommit.getMessage().equals("initial commit")) {
            System.out.println("===");
            System.out.println("commit" + " "
                    + Utils.sha1(Utils.serialize(thiscommit)));
            System.out.println(thiscommit.getTimestamp());
            System.out.println(thiscommit.getMessage());
            System.out.print("\n");
            thiscommit = thiscommit.getParent();
        }
        System.out.println("===");
        System.out.println("commit" + " "
                + Utils.sha1(Utils.serialize(thiscommit)));
        System.out.println("Date: Thu Jan 1 00:00:00 1970 -0800");
        System.out.println(thiscommit.getMessage());
        System.out.print("\n");
    }

    public void globallog() {
        File gitlet = new File(".gitlet");
        for (File each : gitlet.listFiles()) {
            try {
                Commit thiscommit = Utils.readObject(each, Commit.class);
                if (thiscommit.getMessage().equals("initial commit")) {
                    System.out.println("===");
                    System.out.println("commit" + " "
                            + Utils.sha1(Utils.serialize(thiscommit)));
                    System.out.println("Date: Thu Jan 1 00:00:00 1970 -0800");
                    System.out.println(thiscommit.getMessage());
                    System.out.print("\n");
                } else {
                    System.out.println("===");
                    System.out.println("commit" + " "
                            + Utils.sha1(Utils.serialize(thiscommit)));
                    System.out.println(thiscommit.getTimestamp());
                    System.out.println(thiscommit.getMessage());
                    System.out.print("\n");
                }
            } catch (Exception e) {
                String useless = "";
            }
        }
    }

    public void rm(String... args) {
        File staging = Utils.join(".gitlet", "staging");
        TreeMap stagingtree = Utils.readObject(staging, TreeMap.class);
        Boolean iserror = true;
        if (stagingtree.containsKey(args[1])) {
            stagingtree.remove(args[1]);
            iserror = false;
        }
        Utils.writeObject(staging, stagingtree);
        File branchesfile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branches =
                Utils.readObject(branchesfile, TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branches.get(headpointer);
        Commit currentcommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);
        File cwd = new File(System.getProperty("user.dir"));
        if (currentcommit.getCommitedfiles().containsKey(args[1])) {
            File removalstage = Utils.join(".gitlet", "removal");
            @SuppressWarnings("unchecked")
            TreeMap<String, String> removaltree =
                    Utils.readObject(removalstage, TreeMap.class);
            removaltree.put(args[1],
                    currentcommit.getCommitedfiles().get(args[1]));
            Utils.writeObject(removalstage, removaltree);
            File workspacefile = Utils.join(cwd, args[1]);
            workspacefile.delete();
            iserror = false;
        }
        if (iserror) {
            System.out.println("No reason to remove the file.");
        }
    }

    public void statusHelper() {
        System.out.println("\n=== Staged Files ===");
        File staging = Utils.join(".gitlet", "staging");
        TreeMap stagingtree = Utils.readObject(staging, TreeMap.class);
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, String>> stagefiles = stagingtree.entrySet();
        for (Map.Entry<String, String> sfile : stagefiles) {
            System.out.println(sfile.getKey());
        }
        System.out.println("\n=== Removed Files ===");
        File removalfile = Utils.join(".gitlet", "removal");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> removal =
                Utils.readObject(removalfile, TreeMap.class);
        for (Map.Entry<String, String> entry : removal.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
    }

    public void statusHelper2(Commit currentcommit,
                              TreeMap<String, String> stagingtree,
                              TreeMap<String, String> removal) {
        Set<Map.Entry<String, String>> committree =
                currentcommit.getCommitedfiles().entrySet();
        File cwd = new File(System.getProperty("user.dir"));
        ArrayList<String> cwdfilelist = new ArrayList<>();
        for (File cwdfile : cwd.listFiles()) {
            if (!cwdfile.getName().equals(".gitlet") && cwdfile.exists()) {
                cwdfilelist.add(cwdfile.getName());
            }
        }
        for (Map.Entry<String, String> entry : committree) {
            if (!cwdfilelist.contains(entry.getKey())
                    && stagingtree.containsKey(entry.getKey())) {
                System.out.println(entry.getKey() + " " + "(deleted)");
            }
            if (!cwdfilelist.contains(entry.getKey())
                    && !removal.containsKey(entry.getKey())) {
                System.out.println(entry.getKey() + " " + "(deleted)");
            } else {
                File workspacefile = Utils.join(cwd, entry.getKey());
                if (workspacefile.exists()) {
                    String workcontent = Utils.readContentsAsString(
                            workspacefile);
                    String blobid = entry.getValue();
                    File committedBlobfile = Utils.join(".gitlet", blobid);
                    Blob committedBlob =
                            Utils.readObject(committedBlobfile, Blob.class);
                    if (!workcontent.equals(committedBlob.getContent())) {
                        System.out.println(entry.getKey() + " " + "(modified)");
                    }
                }
            }
        }
    }

    public void status() {
        File branches = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchestree =
                Utils.readObject(branches, TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String head = Utils.readContentsAsString(headfile);
        System.out.println("=== Branches ===");
        System.out.println("*" + head);
        Set<Map.Entry<String, String>> entries = branchestree.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!entry.getKey().equals(head)) {
                System.out.println(entry.getKey());
            }
        }
        File cwd = new File(System.getProperty("user.dir"));
        ArrayList<String> cwdfilelist = new ArrayList<>();
        for (File cwdfile : cwd.listFiles()) {
            if (!cwdfile.getName().equals(".gitlet") && cwdfile.exists()) {
                cwdfilelist.add(cwdfile.getName());
            }
        }
        statusHelper();
        File removalfile = Utils.join(".gitlet", "removal");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> removal =
                Utils.readObject(removalfile, TreeMap.class);
        File staging = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingtree =
                Utils.readObject(staging, TreeMap.class);
        String headcommitid = branchestree.get(head);
        Commit currentcommit = Utils.readObject(
                Utils.join(".gitlet", headcommitid), Commit.class);

        statusHelper2(currentcommit, stagingtree, removal);
        System.out.println("\n=== Untracked Files ===");
        for (String cwdfilename : cwdfilelist) {
            if (!stagingtree.containsKey(cwdfilename)
                    && !currentcommit.getCommitedfiles().containsKey(
                            cwdfilename)
                    && !cwdfilename.equals(".gitlet")) {
                System.out.println(cwdfilename);
            }
        }
    }

    public void find(String commitMessage) {
        File gitlet = new File(".gitlet");
        Boolean isError = true;
        for (File each : gitlet.listFiles()) {
            try {
                Commit thisCommit = Utils.readObject(each, Commit.class);
                if (thisCommit.getMessage().equals(commitMessage)) {
                    isError = false;
                    System.out.println(Utils.sha1(Utils.serialize(thisCommit)));
                }
            } catch (Exception e) {
                String useless = "";
            }
        }
        if (isError) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void resetHelper(File thisCommitfile, TreeMap stagingTree,
                            Commit headcommit,
                            TreeMap<String, String> branchesTree,
                            String headpointer, String commitID,
                            File branchesfile) {
        File cwd = new File(System.getProperty("user.dir"));
        Commit thiscommit = Utils.readObject(thisCommitfile, Commit.class);
        Set<Map.Entry<String, String>> entries =
                thiscommit.getCommitedfiles().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String blobsha1 = entry.getValue();
            Blob thisblob = Utils.readObject(
                    Utils.join(".gitlet", blobsha1), Blob.class);
            String content = thisblob.getContent();
            File workspacefile = Utils.join(cwd, entry.getKey());
            try {
                workspacefile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(workspacefile, content);
        }
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, String>> e1 = stagingTree.entrySet();
        for (Map.Entry<String, String> e2 : e1) {
            File targetFile = Utils.join(cwd, e2.getKey());
            if (targetFile.exists()) {
                targetFile.delete();
            }
        }
        for (File each : cwd.listFiles()) {
            if (!thiscommit.getCommitedfiles().containsKey(each.getName())
                    && headcommit.getCommitedfiles().containsKey(
                            each.getName())) {
                File deadfile = Utils.join(cwd, each.getName());
                deadfile.delete();
            }
        }
        branchesTree.put(headpointer, commitID);
        Utils.writeObject(branchesfile, branchesTree);
    }
    public void reset(String commitID) {
        File cwd = new File(System.getProperty("user.dir"));
        File thisCommitfile = Utils.join(".gitlet", commitID);
        if (!thisCommitfile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File branchesfile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree = Utils.readObject(
                branchesfile, TreeMap.class);
        File headfile = Utils.join(".gitlet", "head");
        String headpointer = Utils.readContentsAsString(headfile);
        String headcommitid = branchesTree.get(headpointer);
        Commit headcommit = Utils.readObject(Utils.join(
                ".gitlet", headcommitid), Commit.class);
        File staging = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingTree = Utils.readObject(
                staging, TreeMap.class);
        for (File each : cwd.listFiles()) {
            if (each.getName().equals(".gitlet")) {
                String useless = "";
            } else if (!headcommit.getCommitedfiles().containsKey(
                    each.getName())
                    && !stagingTree.containsKey(each.getName())) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it," + " or add and commit it first.");
                return;
            }
        }
        resetHelper(thisCommitfile, stagingTree, headcommit,
                 branchesTree, headpointer, commitID, branchesfile);

        TreeMap emptyStagingTree = new TreeMap<>();
        Utils.writeObject(staging, emptyStagingTree);
        File removal = Utils.join(".gitlet", "removal");
        TreeMap emptyRemovalTree = new TreeMap<>();
        Utils.writeObject(removal, emptyRemovalTree);
    }

    public void branch(String branchName) {
        File branches = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree =
                Utils.readObject(branches, TreeMap.class);
        if (branchesTree.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        String latestCommitID = branchesTree.get(headPointer);
        branchesTree.put(branchName, latestCommitID);
        Utils.writeObject(branches, branchesTree);
    }

    public void rm(String fileName) {
        Boolean isError = true;
        File stagingFile = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingTree =
                Utils.readObject(stagingFile, TreeMap.class);
        if (stagingTree.containsKey(fileName)) {
            isError = false;
            stagingTree.remove(fileName);
            Utils.writeObject(stagingFile, stagingTree);
        }
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        File branchesFile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree = Utils.readObject(
                branchesFile, TreeMap.class);
        String currentCommitID = branchesTree.get(headPointer);
        File currentCommitFile = Utils.join(".gitlet", currentCommitID);
        Commit currentCommit =
                Utils.readObject(currentCommitFile, Commit.class);
        if (currentCommit.getCommitedfiles().containsKey(fileName)) {
            isError = false;
            File cwd = new File(System.getProperty("user.dir"));
            File workingFile = Utils.join(cwd, fileName);
            if (workingFile.exists()) {
                workingFile.delete();
            }
            File removal = Utils.join(".gitlet", "removal");
            @SuppressWarnings("unchecked")
            TreeMap<String, String> removalTree = Utils.readObject(removal,
                    TreeMap.class);
            removalTree.put(fileName,
                    currentCommit.getCommitedfiles().get(fileName));
            Utils.writeObject(removal, removalTree);
        }
        if (isError) {
            System.out.println("No reason to remove the file.");
        }
    }

    public void rmBranch(String branchName) {
        File branchesFile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree =
                Utils.readObject(branchesFile, TreeMap.class);
        if (!branchesTree.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        if (headPointer.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branchesTree.remove(branchName);
        Utils.writeObject(branchesFile, branchesTree);
    }

    public Boolean mergeIsError1(String branchName) {
        File branchesFile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree =
                Utils.readObject(branchesFile, TreeMap.class);
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        String headCommitID = branchesTree.get(headPointer);
        String thisCommitID = branchesTree.get(branchName);
        File headCommitFile = Utils.join(".gitlet", headCommitID);
        Commit headCommit = Utils.readObject(headCommitFile, Commit.class);
        File cwd = new File(System.getProperty("user.dir"));

        File stagingFile = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingTree =
                Utils.readObject(stagingFile, TreeMap.class);
        File removal = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> removalTree =
                Utils.readObject(removal, TreeMap.class);
        if (!stagingTree.isEmpty() || !removalTree.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!branchesTree.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (headPointer.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        for (File each : cwd.listFiles()) {
            if (each.getName().equals(".gitlet")) {
                String useless = "";
            } else if (
                    !headCommit.getCommitedfiles().containsKey(each.getName())
                    && !stagingTree.containsKey(each.getName())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it," + " or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    public Commit getLatestCommit(String branchName) {
        File branchesFile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree =
                Utils.readObject(branchesFile, TreeMap.class);
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        String headCommitID = branchesTree.get(headPointer);
        String thisCommitID = branchesTree.get(branchName);
        File headCommitFile = Utils.join(".gitlet", headCommitID);
        Commit headCommit = Utils.readObject(headCommitFile, Commit.class);
        ArrayList<Commit> latestCommonList = new ArrayList<>();
        Commit latestCommon = null;
        ArrayList<File> markedFileList = new ArrayList<>();
        Commit targetCommit = headCommit;
        File targetCommitFile = headCommitFile;
        while (!targetCommit.getMessage().equals("initial commit")) {
            targetCommit.mark();
            Utils.writeObject(targetCommitFile, targetCommit);
            markedFileList.add(targetCommitFile);
            if (targetCommit.getSecondParent() != null) {
                File anotherTargetFile = targetCommit.getSecondParent();
                Commit anotherTarget =
                        Utils.readObject(anotherTargetFile, Commit.class);
                anotherTarget.mark();
                Utils.writeObject(anotherTargetFile, anotherTarget);
                markedFileList.add(anotherTargetFile);
            }
            targetCommitFile = targetCommit.getParentFile();
            targetCommit = targetCommit.getParent();
        }
        targetCommit.mark();
        Utils.writeObject(targetCommitFile, targetCommit);
        Commit initialCommit = targetCommit;
        File thisCommitFile = Utils.join(".gitlet", thisCommitID);
        Commit thisCommit = Utils.readObject(thisCommitFile, Commit.class);
        Commit t2 = thisCommit;
        while (!t2.getMessage().equals("initial commit")) {
            if (t2.isMarked()) {
                latestCommonList.add(t2);
            }
            if (t2.getSecondParent() != null) {
                File anotherTargetFile = t2.getSecondParent();
                Commit anotherTarget =
                        Utils.readObject(anotherTargetFile, Commit.class);
                if (anotherTarget.isMarked()) {
                    latestCommonList.add(anotherTarget);
                }
            }
            t2 = t2.getParent();
        }
        if (latestCommonList.isEmpty()) {
            latestCommon = initialCommit;
        }
        latestCommon = findMinPath(headCommit, latestCommonList,
                latestCommon, markedFileList);
        return latestCommon;
    }

    public Commit findMinPath(Commit headCommit,
                              ArrayList<Commit> latestCommonList,
                              Commit latestCommon,
                              ArrayList<File> markedFileList) {
        Commit closeToHead = headCommit;
        int min = 100;
        for (int i = 0; i < latestCommonList.size(); i++) {
            Commit aCommon = latestCommonList.get(i);
            int disForCommon = 0;
            while (!closeToHead.getMessage().equals(aCommon.getMessage())
                    && !closeToHead.getMessage().equals("initial commit")) {
                disForCommon += 1;
                closeToHead = closeToHead.getParent();
            }
            if (disForCommon < min) {
                min = disForCommon;
                latestCommon = aCommon;
            }
        }
        for (File each : markedFileList) {
            Commit eachCommit = Utils.readObject(each, Commit.class);
            eachCommit.clearMark();
            Utils.writeObject(each, eachCommit);
        }
        return latestCommon;
    }

    public ArrayList<String> getEverything(File each, String branchName,
                                   Commit thisCommit, Commit headCommit,
                                   Commit latestCommon) {
        ArrayList<String> result = new ArrayList<>();
        String thisBlobID = thisCommit.getCommitedfiles().get(each.getName());
        String thisBlobContent = null;
        if (!(thisBlobID == null)) {
            File thisBlobFile = Utils.join(".gitlet", thisBlobID);
            thisBlobContent =
                    Utils.readObject(thisBlobFile, Blob.class).getContent();
        } else {
            thisBlobContent = "";
        }
        result.add(thisBlobContent);
        String headBlobID = headCommit.getCommitedfiles().get(each.getName());
        String headBlobContent = null;
        if (!(headBlobID == null)) {
            File headBlobFile = Utils.join(".gitlet", headBlobID);
            headBlobContent =
                    Utils.readObject(headBlobFile, Blob.class).getContent();
        } else {
            headBlobContent = "";
        }
        result.add(headBlobContent);
        String splitPointBlobID =
                latestCommon.getCommitedfiles().get(each.getName());
        String splitPointBlobContent = null;
        if (!(splitPointBlobID == null)) {
            File splitPointBlobFile =
                    Utils.join(".gitlet", splitPointBlobID);
            splitPointBlobContent = Utils.readObject(
                    splitPointBlobFile, Blob.class).getContent();
        } else {
            splitPointBlobContent = "";
        }
        result.add(splitPointBlobContent);
        result.add(thisBlobID);
        result.add(headBlobID);
        result.add(splitPointBlobID);
        return result;
    }

    public ArrayList<File> getAllFile(Commit thisCommit, Commit headCommit) {
        File cwd = new File(System.getProperty("user.dir"));
        ArrayList<File> allFile = new ArrayList<>();
        for (File e : cwd.listFiles()) {
            allFile.add(e);
        }
        Set<Map.Entry<String, String>> entries =
                thisCommit.getCommitedfiles().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!headCommit.getCommitedfiles().containsKey(entry.getKey())) {
                File additionalFile = Utils.join(cwd, entry.getKey());
                allFile.add(additionalFile);
            }
        }
        return allFile;
    }

    public void doConflict(String headBlobContent, String thisBlobContent,
                           TreeMap<String, String> stagingTree,
                           File each, File stagingFile) {
        String newContent =
                "<<<<<<< HEAD\n"
                        + headBlobContent
                        + "=======\n"
                        + thisBlobContent
                        + ">>>>>>>\n";
        Utils.writeContents(each, newContent);
        Blob newBlob = new Blob(each);
        File newBlobFile = Utils.join(".gitlet",
                Utils.sha1(Utils.serialize(newBlob)));
        try {
            newBlobFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(newBlobFile, newBlob);
        stagingTree.put(each.getName(), Utils.sha1(Utils.serialize(newBlob)));
        Utils.writeObject(stagingFile, stagingTree);
        System.out.println("Encountered a merge conflict.");
    }

    public void doNotConflict(Commit latestCommon, File each,
                              String thisBlobContent, String headBlobContent,
                              String splitPointBlobContent, String thisCommitID,
                              String thisBlobID,
                              TreeMap<String, String> stagingTree) {
        File stagingFile = Utils.join(".gitlet", "staging");
        if (latestCommon.getCommitedfiles().containsKey(each.getName())) {
            if (thisBlobContent.equals("")
                    && (headBlobContent.equals(splitPointBlobContent))) {
                this.rm(each.getName());
            } else if (!thisBlobContent.equals(splitPointBlobContent)
                    && (headBlobContent.equals(splitPointBlobContent))) {
                String[] checkoutMessage2 = {"checkout", thisCommitID, "--",
                    each.getName()};
                this.checkout(checkoutMessage2);
                stagingTree.put(each.getName(), thisBlobID);
                Utils.writeObject(stagingFile, stagingTree);
            }
        } else {
            if (headBlobContent.equals("") && !thisBlobContent.equals("")) {
                String[] checkoutMessage2 = {"checkout",
                    thisCommitID, "--", each.getName()};
                this.checkout(checkoutMessage2);
                stagingTree.put(each.getName(), thisBlobID);
                Utils.writeObject(stagingFile, stagingTree);
            }
        }
    }

    public void merge(String branchName) {
        File branchesFile = Utils.join(".gitlet", "branches");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchesTree =
                Utils.readObject(branchesFile, TreeMap.class);
        File headFile = Utils.join(".gitlet", "head");
        String headPointer = Utils.readContentsAsString(headFile);
        String headCommitID = branchesTree.get(headPointer);
        String thisCommitID = branchesTree.get(branchName);
        File headCommitFile = Utils.join(".gitlet", headCommitID);
        Commit headCommit = Utils.readObject(headCommitFile, Commit.class);
        File stagingFile = Utils.join(".gitlet", "staging");
        @SuppressWarnings("unchecked")
        TreeMap<String, String> stagingTree =
                Utils.readObject(stagingFile, TreeMap.class);
        if (mergeIsError1(branchName)) {
            return;
        }
        File thisCommitFile = Utils.join(".gitlet", thisCommitID);
        Commit thisCommit = Utils.readObject(thisCommitFile, Commit.class);
        Commit latestCommon = getLatestCommit(branchName);
        if (latestCommon.getMessage().equals(thisCommit.getMessage())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        } else if (latestCommon.getMessage().equals(headCommit.getMessage())) {
            String[] checkoutMessage = {"checkout ", branchName};
            this.checkout(checkoutMessage);
            System.out.println("Current branch fast-forwarded.");
            return;
        } else {
            boolean isInConflict = false;
            ArrayList<File> allFile = getAllFile(thisCommit, headCommit);
            for (File each : allFile) {
                if (!each.getName().equals(".gitlet")) {
                    ArrayList<String> everything = getEverything(each,
                            branchName, thisCommit, headCommit, latestCommon);
                    String thisBlobContent = everything.get(0);
                    String headBlobContent = everything.get(1);
                    String splitPointBlobContent = everything.get(2);
                    String thisBlobID = everything.get(3);
                    boolean cond3 =
                            (!headBlobContent.equals(splitPointBlobContent)
                            && !thisBlobContent.equals(splitPointBlobContent));
                    boolean cond4 = (!headBlobContent.equals(thisBlobContent));
                    isInConflict = (cond3 && cond4);
                    if (isInConflict) {
                        doConflict(headBlobContent, thisBlobContent,
                                stagingTree, each, stagingFile);
                    } else {
                        doNotConflict(latestCommon, each, thisBlobContent,
                                headBlobContent, splitPointBlobContent,
                                thisCommitID, thisBlobID, stagingTree);
                    }
                }
            }
            this.commit("Merged " + branchName + " into " + headPointer + ".");
        }
    }
}








