package gitlet;

import java.io.Serializable;

/** Branch class.
 * @author yuxinye
 */
public class Branch implements Serializable {
    /** branch name. */
    private String _name;
    /** current commit. */
    private Commit _currentCommit;

    /** Constructor.
     * @param name branch name
     * @param currentCommit current commit
     */
    public Branch(String name, Commit currentCommit) {
        _name = name;
        _currentCommit = currentCommit;
    }

    /** Get the branch name.
     * @return branch name
     */
    public String getName() {
        return _name;
    }

    /** Get the commit.
     * @return current commit
     */
    public Commit getCommit() {
        return _currentCommit;
    }
}
