package com.gentics.cr.lucene.indexer.compress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gentics.cr.lucene.indexer.index.LockedIndexException;
import com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation;
import com.gentics.cr.util.file.ArchiverUtil;
import com.gentics.cr.util.indexing.IndexControllerSingleton;
import com.gentics.cr.util.indexing.IndexLocation;

/**
 * Creates an archive of the index.
 * @author voglerc
 *l
 */
public class IndexCompressor {

	/**
	 * Logging.
	 */
	protected static final Logger LOGGER = Logger.getLogger(IndexCompressor.class);

	/**
	 * Compress the given index.
	 * @param index indexname
	 */
	public void compress(final String index) {
		ConcurrentHashMap<String, IndexLocation> indexLocations = IndexControllerSingleton.getIndexControllerInstance().getIndexes();
		IndexLocation location = indexLocations.get(index);
		if (location instanceof LuceneSingleIndexLocation) {
			LuceneSingleIndexLocation indexLocation = (LuceneSingleIndexLocation) location;
			File indexDirectory = new File(indexLocation.getReopenFilename()).getParentFile();
			File writeLock = null;
			boolean weWroteTheWriteLock = false;
			try {
				indexLocation.checkLock();
				if (indexDirectory.canWrite()) {
					writeLock = new File(indexDirectory, "write.lock");
					if (writeLock.createNewFile()) {
						weWroteTheWriteLock = true;
					} else {
						throw new LockedIndexException(new Exception("the write lock file already exists in the index."));
					}
					//set to read only so the index jobs will not delete it.
					writeLock.setReadOnly();

					File compressedIndexDirectory = indexDirectory.getParentFile();
					File compressedIndexFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/")
							.append(index).append(".tar.gz").toString());
					if (compressedIndexFile.exists()) {
						compressedIndexFile.delete();
					}
					FileOutputStream fileOutputStream = new FileOutputStream(compressedIndexFile);
					ArchiverUtil.generateGZippedTar(fileOutputStream, indexDirectory);
				} else {
					LOGGER.error("Cannot lock the index directory to ensure the consistency of the archive.");
				}
			} catch (IOException e) {
				LOGGER.error("Cannot generate the archive correctly.", e);
			} catch (LockedIndexException e) {
				LOGGER.error("Cannot generate the archive while the index is locked.", e);
			} finally {
				if (writeLock != null && writeLock.exists() && weWroteTheWriteLock) {
					writeLock.delete();
				}
			}
		} else {
			LOGGER.error("generating an archive for " + location + " not supported yet.");
		}
	}

}
