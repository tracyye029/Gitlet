package gitlet;

import java.io.Serializable;
import java.util.LinkedHashMap;

/** Staging area class.
 * @author yuxinye
 */
public class StagingArea implements Serializable {
    /** addition area. */
    private LinkedHashMap<String, String> addition;
    /** removal area. */
    private LinkedHashMap<String, String> removal;

    /** Constructor. */
    public StagingArea() {
        addition = new LinkedHashMap<>();
        removal = new LinkedHashMap<>();
    }

    /** Get added files.
     * @return added files
     */
    public LinkedHashMap<String, String> getAddedFiles() {
        return addition;
    }

    /** Get removed files.
     * @return removed files
     */
    public LinkedHashMap<String, String> getRemovedFiles() {
        return removal;
    }

    /** add files in to addtion area.
     * @param fileName name of file
     * @param sha file blob ID
     */
    public void addFiles(String fileName, String sha) {
        addition.put(fileName, sha);
    }

    /** add files in to removal area.
     * @param fileName name of file
     * @param sha file blob ID
     */
    public void addFilesToRemoval(String fileName, String sha) {
        removal.put(fileName, sha);
    }

}
