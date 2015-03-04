package nl.weeaboo.vn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import nl.weeaboo.collections.LRUSet;
import nl.weeaboo.vn.IEnvironment;
import nl.weeaboo.vn.INotifier;

public abstract class ResourceLoader {

    protected final INotifier notifier;

    private final Set<String> checkedFilenames;

    private String[] autoFileExts = new String[0];
    private boolean checkFileExt;

    public ResourceLoader(IEnvironment env) {
        this.notifier = env.getNotifier();
        this.checkedFilenames = new LRUSet<String>(128);
    }

    //Functions
    protected String replaceExt(String filename, String ext) {
        return BaseImpl.replaceExt(filename, ext);
    }

    protected String normalizeFilename(String filename) {
        if (filename == null) return null;

        if (isValidFilename(filename)) {
            return filename; //The given extension works
        }

        for (String ext : autoFileExts) {
            String fn = replaceExt(filename, ext);
            if (isValidFilename(fn)) {
                return fn; //This extension works
            }
        }

        return null;
    }

    public void checkRedundantFileExt(String filename) {
        if (filename == null || !checkFileExt) {
            return;
        }

        if (!checkedFilenames.add(filename)) {
            return;
        }

        //Check if a file extension in the default list has been specified.
        for (String ext : autoFileExts) {
            if (filename.endsWith("." + ext)) {
                if (isValidFilename(filename)) {
                    notifier.debug("You don't need to specify the file extension: " + filename);
                } else if (isValidFilename(normalizeFilename(filename))) {
                    notifier.warn("Incorrect file extension: " + filename);
                }
                break;
            }
        }
    }

    public void preload(String filename) {
        preload(filename, false);
    }

    public void preload(String filename, boolean suppressErrors) {
        if (!suppressErrors) {
            checkRedundantFileExt(filename);
        }

        String normalized = normalizeFilename(filename);
        if (normalized != null) {
            preloadNormalized(normalized);
        }
    }

    protected abstract void preloadNormalized(String filename);

    //Getters
    /**
     * @param normalizedFilename A normalized filename
     */
    protected abstract boolean isValidFilename(String normalizedFilename);

    protected Collection<String> getMediaFiles(String folder) {
        Collection<String> files = getFiles(folder);
        List<String> filtered = new ArrayList<String>(files.size());
        for (String file : files) {
            if (isValidFilename(file)) {
                filtered.add(file);
            }
        }
        return filtered;
    }

    protected abstract List<String> getFiles(String folder);

    //Setters
    public void setAutoFileExts(String... exts) {
        autoFileExts = exts.clone();
    }

}