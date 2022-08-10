package gitlet;

import java.io.Serializable;

/** Remote class.
 * @author yuxinye
 */
public class Remote implements Serializable {
    /** remote path. */
    private String _remotePath;

    /** Constructor.
     * @param remoteName remote name.
     * @param remotePath remote path.
     */
    public Remote(String remoteName, String remotePath) {
        _remotePath = remotePath;
    }

    /** Get remote path.
     * @return remote path. */
    public String getRemotePath() {
        return _remotePath;
    }
}
