package com.gentics.cr.lucene.indexer.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.gentics.cr.lucene.didyoumean.DidyoumeanIndexExtension;
import com.gentics.cr.lucene.indexer.index.LockedIndexException;
import com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation;
import com.gentics.cr.util.file.ArchiverUtil;
import com.gentics.cr.util.indexing.IndexControllerSingleton;
import com.gentics.cr.util.indexing.IndexExtension;
import com.gentics.cr.util.indexing.IndexLocation;
import com.gentics.lib.genericexceptions.NotYetImplementedException;

/**
 * Creates an archive of the index.
 * @author voglerc
 */
public class IndexCompressor extends Thread {

	/**
	 * Data class holding information about the index location and the index directory on the FS. 
	 * @author Klaus Pfeiffer
	 */
	private class IndexCompressorStructure {

		/**
		 * IndexLocation.
		 */
		private final LuceneSingleIndexLocation indexLocation;

		/**
		 * directory of index-files.
		 */
		private final File indexDirectory;

		/**
		 * @param indexLocation lucene index location
		 */
		public IndexCompressorStructure(final IndexLocation indexLocation) {
			super();
			if (indexLocation instanceof LuceneSingleIndexLocation) {
				LuceneSingleIndexLocation luceneSingleIndexLocation = (LuceneSingleIndexLocation) indexLocation;
				this.indexLocation = luceneSingleIndexLocation;
				this.indexDirectory = new File(this.indexLocation.getReopenFilename()).getParentFile();
			} else {
				throw new NotYetImplementedException("Only LuceneSingleIndexLocation is implemented by now.");
			}
		}

		public LuceneSingleIndexLocation getIndexLocation() {
			return indexLocation;
		}

		public File getIndexDirectory() {
			return indexDirectory;
		}
	}

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

	private List<IndexCompressorStructure> indexDirectoryStructures = new ArrayList<IndexCompressor.IndexCompressorStructure>();

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
			LuceneSingleIndexLocation indexLocation = (LuceneSingleIndexLocation) location;

			// root directory of all indexes
			File compressedIndexDirectory = new File(indexLocation.getReopenFilename()).getParentFile().getParentFile();

			// prepare file objects
			compressedIndexFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/").append(index)
					.append(".tar.gz").toString());
			compressedIndexTempFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/").append(index)
					.append(".tmp").toString());

			indexDirectoryStructures.add(new IndexCompressorStructure(indexLocation));

			// look for DYM extension
			for (IndexExtension ie : indexLocation.getExtensions().values()) {
				if (ie instanceof DidyoumeanIndexExtension) {
					// found DYM index extension
					DidyoumeanIndexExtension dym = (DidyoumeanIndexExtension) ie;
					indexDirectoryStructures.add(new IndexCompressorStructure(dym.getDidyoumeanLocation()));
					break;
				}
			}

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
			checkLock();
			if (checkCanWrite()) {
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

				ArchiverUtil.generateGZippedTar(fileOutputStream, getIndexDirectories());

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
			removeIndexLock();
		}
	}

	private File[] getIndexDirectories() {
		List<File> files = new ArrayList<File>(indexDirectoryStructures.size());
		for (IndexCompressorStructure struc : indexDirectoryStructures) {
			files.add(struc.getIndexDirectory());
		}
		return files.toArray(new File[indexDirectoryStructures.size()]);
	}

	private boolean checkCanWrite() {
		for (IndexCompressorStructure struc : indexDirectoryStructures) {
			if (!struc.getIndexDirectory().canWrite()) {
				return false;
			}
		}
		return true;
	}

	private void checkLock() throws LockedIndexException {
		for (IndexCompressorStructure struc : indexDirectoryStructures) {
			struc.getIndexLocation().checkLock();
		}
	}

	/**
	 * Locks the index.
	 * @throws IOException if the lock could not be written
	 * @throws LockedIndexException if index is already locked 
	 */
	private void lockIndex() throws IOException, LockedIndexException {
		if (indexDirectoryStructures.size() == 0) {
			throw new IndexOutOfBoundsException("Size of indexDirectoryStructures is zero.");
		}
		writeLock = new File(indexDirectoryStructures.get(0).getIndexDirectory(), "write.lock");
		if (writeLock.createNewFile()) {
			weWroteTheWriteLock = true;
		} else {
			throw new LockedIndexException(new Exception("the write lock file already exists in the index."));
		}
		//set to read only so the index jobs will not delete it.
		writeLock.setReadOnly();
	}

	private void removeIndexLock() {
		if (writeLock != null && writeLock.exists() && weWroteTheWriteLock) {
			writeLock.delete();
		}
	}

}
