package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author yuxinye
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Gitlet gitlet = new Gitlet();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else if (args.length == 1 && args[0].equals("init")) {
            gitlet.init();
        } else if (!gitlet.GITLET_DIC.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (args.length == 2 && args[0].equals("add")) {
            gitlet.add(args[1]);
        } else if (args.length == 2 && args[0].equals("commit")) {
            gitlet.commit(args[1]);
        } else if (args.length == 2 && args[0].equals("rm")) {
            gitlet.rm(args[1]);
        } else if (args.length == 1 && args[0].equals("log")) {
            gitlet.log();
        } else if (args.length == 1 && args[0].equals("global-log")) {
            gitlet.globalLog();
        } else if (args.length == 2 && args[0].equals("find")) {
            gitlet.find(args[1]);
        } else if (args.length == 1 && args[0].equals("status")) {
            gitlet.status();
        } else if (args[0].equals("checkout")) {
            if ((args.length != 2 && args.length != 3 && args.length != 4)
                    || (args.length == 3 && !args[1].equals("--"))
                    || (args.length == 4 && !args[2].equals("--"))) {
                System.out.println("Incorrect Operands.");
            } else {
                gitlet.checkOut(args);
            }
        } else if (args.length == 2 && args[0].equals("branch")) {
            gitlet.branch(args[1]);
        } else if (args.length == 2 && args[0].equals("rm-branch")) {
            gitlet.rmBranch(args[1]);
        } else if (args.length == 2 && args[0].equals("reset")) {
            gitlet.reset(args[1]);
        } else if (args.length == 2 && args[0].equals("merge")) {
            gitlet.merge(args[1]);
        } else if (args.length == 3 && args[0].equals("add-remote")) {
            gitlet.addRemote(args);
        } else if (args.length == 2 && args[0].equals("rm-remote")) {
            gitlet.rmRemote(args[1]);
        } else if (args.length == 3 && args[0].equals("push")) {
            gitlet.push(args[1], args[2]);
        } else if (args.length == 3 && args[0].equals("fetch")) {
            gitlet.fetch(args[1], args[2]);
        } else if (args.length == 3 && args[0].equals("pull")) {
            gitlet.pull(args[1], args[2]);
        } else {
            System.out.println("No command with that name exists.");
        }
    }

}
