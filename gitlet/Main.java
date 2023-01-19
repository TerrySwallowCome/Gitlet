package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Tianyu Liu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        GitletSystem thisSystem = new GitletSystem();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        File cwd = new File(System.getProperty("user.dir"));
        if (!args[0].equals("init")) {
            if (!Utils.join(cwd, ".gitlet").exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
        }
        if (args[0].equals("add")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return;
            }
            thisSystem.add(args[1]);
            return;
        }
        if (args[0].equals("init")) {
            if (!checkNumArgs(args, 1)) {
                System.out.println("Incorrect operands.");
                return;
            }
            thisSystem.init();
            return;
        }
        if (args[0].equals("commit")) {
            if (args.length < 2 || args[1].equals("")) {
                System.out.println("Please enter a commit message.");
            } else {
                thisSystem.commit(args[1]);
            }
            return;
        }
        if (args[0].equals("checkout")) {
            if (!checkNumArgs(args, 2)
                    && !checkNumArgs(args, 3)
                    && !checkNumArgs(args, 4)) {
                System.out.println("Incorrect operands.");
                return;
            }
            thisSystem.checkout(args);
            return;
        }
        if (isExecuted(args, thisSystem)) {
            return;
        }
        if (isExecuted2(args, thisSystem)) {
            return;
        } else {
            System.out.println("No command with that name exists.");
        }
    }

    public static boolean isExecuted2(String[] args, GitletSystem thisSystem) {
        if (args[0].equals("rm")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.rm(args[1]);
            return true;
        }
        if (args[0].equals("rm-branch")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.rmBranch(args[1]);
            return true;
        }
        if (args[0].equals("log")) {
            if (!checkNumArgs(args, 1)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.log();
            return true;
        }
        if (args[0].equals("status")) {
            if (!checkNumArgs(args, 1)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.status();
            return true;
        }
        if (args[0].equals("merge")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.merge(args[1]);
            return true;
        }
        return false;
    }

    public static boolean isExecuted(String[] args, GitletSystem thisSystem) {
        if (args[0].equals("global-log")) {
            if (!checkNumArgs(args, 1)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.globallog();
            return true;
        }
        if (args[0].equals("find")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.find(args[1]);
            return true;
        }
        if (args[0].equals("reset")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.reset(args[1]);
            return true;
        }
        if (args[0].equals("branch")) {
            if (!checkNumArgs(args, 2)) {
                System.out.println("Incorrect operands.");
                return true;
            }
            thisSystem.branch(args[1]);
            return true;
        }
        return false;
    }

    public static boolean checkNumArgs(String[] args, int num) {
        if (args.length != num) {
            return false;
        }
        return true;
    }
}
