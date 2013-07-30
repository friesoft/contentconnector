package com.gentics.cr.lucene.indexer.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.gentics.cr.lucene.indexer.index.LockedIndexException;
import com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation;
import com.gentics.cr.util.file.ArchiverUtil;
import com.gentics.cr.util.indexing.IndexControllerSingleton;
import com.gentics.cr.util.indexing.IndexLocation;
import com.gentics.lib.genericexceptions.NotYetImplementedException;

/**
 * Creates an archive of the index.
 * @author voglerc
 */
public class IndexCompressor extends Thread {

	/**
	 * Logging.
	 */
	protected static final Logger LOGGER = Logger.getLogger(IndexCompressor.class);

	/**
	 * maximum age of the temp-file (minutes).
	 */
	private static final int MAXTEMPFILEAGE = 30;

	/**
	 * index-name.
	 */
	private String index;

	/**
	 * IndexLocation.
	 */
	private LuceneSingleIndexLocation indexLocation;

	/**
	 * directory of index-files.
	 */
	private File indexDirectory;

	/**
	 * compressed index file.
	 */
	private File compressedIndexFile;

	/**
	 * compressed temp file.
	 */
	private File compressedIndexTempFile;

	/**
	 * Writelock-File.
	 */
	private File writeLock = null;

	/**
	 * indicates that the lock was written by this compressor.
	 */
	private boolean weWroteTheWriteLock;

	/**
	 * Creates an new IndexCompressor for the given index.
	 * @param index index-name
	 */
	public IndexCompressor(final String index) {
		super("IndexCompressor: " + index);
		this.index = index;
		setDaemon(true);

		// calculate Paths
		ConcurrentHashMap<String, IndexLocation> indexLocations = IndexControllerSingleton.getIndexControllerInstance().getIndexes();
		IndexLocation location = indexLocations.get(index);
		if (location instanceof LuceneSingleIndexLocation) {
			indexLocation = (LuceneSingleIndexLocation) location;
			indexDirectory = new File(indexLocation.getReopenFilename()).getParentFile();
			File compressedIndexDirectory = indexDirectory.getParentFile();
			compressedIndexFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/").append(index)
					.append(".tar.gz").toString());
			compressedIndexTempFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/").append(index)
					.append(".tmp").toString());
		} else {
			LOGGER.error("generating an archive for " + location + " not supported yet.");
			throw new NotYetImplementedException("generating an archive for " + location + " not supported yet.");
		}
	}

	@Override
	public void run() {
		compress(index);
	}

	/**
	 * Compress the given index.
	 * @param index indexname
	 */
	public void compress(final String index) {
		try {
			LOGGER.info("Start compressing index " + index);
			indexLocation.checkLock();
			if (indexDirectory.canWrite()) {
				lockIndex();

				if (compressedIndexTempFile.exists()) {
					if ((new Date().getTime() - compressedIndexTempFile.lastModified()) / 1000 / 60 > MAXTEMPFILEAGE) {
						// if temp-file is MAXTEMPFILEAGE not modified delete it
						compressedIndexTempFile.delete();
					} else {
						// a compress is in progress => do nothing
						return;
					}
				}
				// compress index and save in temp-file
				FileOutputStream fileOutputStream = new FileOutputStream(compressedIndexTempFile);
				ArchiverUtil.generateGZippedTar(fileOutputStream, indexDirectory);
				// move temp-file to real file
				IOUtils.copy(new FileInputStream(compressedIndexTempFile), new FileOutputStream(compressedIndexFile));
				// delete the temp-file
				if (compressedIndexTempFile.exists()) {
					compressedIndexTempFile.delete();
				}
				LOGGER.info("Compressing index " + index + " finished");
			} else {
				LOGGER.error("Cannot lock the index directory (" + index + ") to ensure the consistency of the archive.");
			}
		} catch (IOException e) {
			LOGGER.error("Cannot generate the archive (" + index + ") correctly.", e);
		} catch (LockedIndexException e) {
			LOGGER.error("Cannot generate the archive (" + index + ") while the index is locked.", e);
		} finally {
			if (writeLock != null && writeLock.exists() && weWroteTheWriteLock) {
				writeLock.delete();
			}
		}
	}

	/**
	 * Locks the index.
	 * @throws IOException if the lock could not be written
	 * @throws LockedIndexException if index is already locked 
	 */
	private void lockIndex() throws IOException, LockedIndexException {
		writeLock = new File(indexDirectory, "write.lock");
		if (writeLock.createNewFile()) {
			weWroteTheWriteLock = true;
		} else {
			throw new LockedIndexException(new Exception("the write lock file already exists in the index."));
		}
		//set to read only so the index jobs will not delete it.
		writeLock.setReadOnly();
	}

}
