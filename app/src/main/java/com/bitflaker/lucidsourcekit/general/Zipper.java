package com.bitflaker.lucidsourcekit.general;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zipper {
    private static final int BUFFER_SIZE = 2048;

    public static void createZipFile(String[] files, OutputStream zipFile) throws IOException {
        byte[] data = new byte[BUFFER_SIZE];
        try (ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(zipFile))) {
            for (String file : files) {
                FileInputStream fileInputStream = new FileInputStream(file);
                try (BufferedInputStream inputStream = new BufferedInputStream(fileInputStream, BUFFER_SIZE)) {
                    outputStream.putNextEntry(new ZipEntry(new File(file).getName()));
                    int bytesRead;
                    while ((bytesRead = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                        outputStream.write(data, 0, bytesRead);
                    }
                }
            }
        }
    }

    public static boolean unzipFile(InputStream zipFile, String extractPath) {
        byte[] data = new byte[BUFFER_SIZE];
        try {
            // Make sure extractPath ends with `File.separator` to make sure it is a directory and
            // to be able to just concat the filename in the code below for extracting
            if (!extractPath.endsWith(File.separator)) {
                extractPath += File.separator;
            }
            File extractLocation = new File(extractPath);
            if(!extractLocation.exists() && !extractLocation.mkdirs()) {
                throw new IOException("Unable to create extraction location directories! Path: \"" + extractLocation.getAbsolutePath() + "\"");
            }

            try (ZipInputStream inputStream = new ZipInputStream(zipFile)) {
                ZipEntry entry;
                while ((entry = inputStream.getNextEntry()) != null) {
                    File unzippedFile = new File(extractPath + entry.getName());

                    // Check if the current entry is a directory, if so, create the directory
                    if (entry.isDirectory()) {
                        if(!unzippedFile.exists() && !unzippedFile.mkdirs()) {
                            throw new IOException("Unable to create directory while trying to extract directory to location \"" + unzippedFile.getAbsolutePath() + "\"");
                        }
                        continue;
                    }

                    // The current entry has to be a file, therefore make sure all parent directories are created
                    File parentDirectory = unzippedFile.getParentFile();
                    if (parentDirectory != null && !parentDirectory.exists() && !parentDirectory.mkdirs()) {
                        throw new IOException("Unable to create parent directories while trying to extract file to location \"" + unzippedFile.getAbsolutePath() + "\"");
                    }

                    // Extract file and write to disk
                    try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(unzippedFile, false), BUFFER_SIZE)) {
                        int bytesRead;
                        while((bytesRead = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                            bufferedOutputStream.write(data, 0, bytesRead);
                        }
                        inputStream.closeEntry();
                    }
                    catch (Exception e) {
                        Log.e("ZIPPER", "Failed to extract file \"" + unzippedFile.getAbsolutePath() + "\"", e);
                        return false;
                    }
                }
                return true;
            }
        }
        catch (Exception e) {
            Log.e("ZIPPER", "Failed to unzip file", e);
            return false;
        }
    }
}
