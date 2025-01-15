package com.server.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;

/**
 * Handles file operations within a specified web root directory for a web server.
 */
public class WebRootHandler {
    private final File webRoot;

    public WebRootHandler(String webRootPath) throws WebRootNotFoundException {
        webRoot = new File(webRootPath);
        if (!webRoot.exists() || !webRoot.isDirectory()) {
            throw new WebRootNotFoundException("Webroot provided does not exist or is not a folder");
        }
    }

    /**
     * Checks to see if the relative path provided exists inside the webroot directory.
     *
     * @param relativePath The relative path to the file
     * @return true if the path exists inside webroot
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean doesProvidedRelativePathExists(String relativePath) {
        File file = new File(webRoot, relativePath);
        if (!file.exists())
            return false;
        try {
            if (file.getCanonicalPath().startsWith(webRoot.getCanonicalPath())) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Get the MIME type of the target file in the given path.
     *
     * @param relativePath The relative path to the file
     * @return MIME type of the file
     */
    public String getFileMimeType(String relativePath) throws FileNotFoundException {
        File file = getFileByPath(relativePath);
        String mimeType = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
        if (mimeType == null) {
            return "application/octet-stream";
        }
        return mimeType;
    }

    /**
     * Returns a byte array of the content of a file.
     *
     * @param relativePath The relative path to the file
     * @return a byte array of the data
     */
    public byte[] getFileByteArrayData(String relativePath) throws FileNotFoundException, ReadFileException {
        File file = getFileByPath(relativePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        try {
            fileInputStream.read(fileBytes);
            fileInputStream.close();
        } catch (IOException e) {
            throw new ReadFileException(e);
        }
        return fileBytes;
    }

    /**
     * Get the target file by relative path in the webroot.
     *
     * @param relativePath The relative path to the file
     * @return File object created from the relative path
     */
    private File getFileByPath(String relativePath) throws FileNotFoundException {
        if (relativePath.endsWith("/")) {
            relativePath += "index.html"; // By default, serve the index.html if it exists.
        }
        if (!doesProvidedRelativePathExists(relativePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        return new File(webRoot, relativePath);
    }
}
