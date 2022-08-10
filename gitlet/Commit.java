package gitlet;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Commit class.
 * @author yuxinye
 */
public class Commit implements Serializable {
    /** log message. */
    private String _logMessage;
    /** time stamp. */
    private String _timeStamp;
    /** commit ID. */
    private String _mySHA;
    /** parent commit ID. */
    private String _parentSHA;
    /** merged parent commit ID. */
    private String _mergedParentSHA;
    /** collection of blobs. */
    private LinkedHashMap<String, String> _blobs;

    /** Constructor.
     * @param logMessage log message
     * @param parentSHA parent commit ID
     * @param mergedParentSHA merged parent commit ID
     * @param blobs collection of blobs
     */
    public Commit(String logMessage, String parentSHA,
                  String mergedParentSHA, LinkedHashMap<String, String> blobs) {
        SimpleDateFormat date = new
                SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        _timeStamp = date.format(new Date());
        _logMessage = logMessage;
        _parentSHA = parentSHA;
        _mySHA = Utils.sha1(Utils.serialize(this));
        _blobs = blobs;
        _mergedParentSHA = mergedParentSHA;
    }

    /** Get time.
     * @return time
     */
    public String getTime() {
        return _timeStamp;
    };

    /** Get log message.
     * @return log message
     */
    public String getMessage() {
        return _logMessage;
    }

    /** Get my commit ID.
     * @return commit ID
     */
    public String getMySHA() {
        return _mySHA;
    }

    /** Get parent ID.
     * @return parent commit ID
     */
    public String getParentSHA() {
        return _parentSHA;
    }

    /** Get merged parent ID.
     * @return merged parent ID
     */
    public String getMergedParentSHA() {
        return _mergedParentSHA;
    }

    /** Get blobs.
     * @return collection of blobs
     */
    public LinkedHashMap<String, String> getBlobs() {
        return _blobs;
    }
}
