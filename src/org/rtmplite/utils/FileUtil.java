package org.rtmplite.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	
	private final static Logger log = LoggerFactory.getLogger(FileUtil.class);
	
	/**
	 * Reads all the bytes of a given file into an array. If the file size exceeds Integer.MAX_VALUE, it will
	 * be truncated.
	 * 
	 * @param localSwfFile
	 * @return file bytes
	 */
	public static byte[] readAsByteArray(File localSwfFile) {
		byte[] fileBytes = new byte[(int) localSwfFile.length()];
		byte[] b = new byte[1];
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(localSwfFile);
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				if (fis.read(b) != -1) {
					fileBytes[i] = b[0];
				} else {
					break;
				}
			}
		} catch (IOException e) {
			log.warn("Exception reading file bytes", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
		return fileBytes;
	}

}
