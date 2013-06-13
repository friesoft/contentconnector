package com.gentics.cr.lucene.indexer.index;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.Directory;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.monitoring.MonitorFactory;
import com.gentics.cr.monitoring.UseCase;
import com.gentics.cr.util.indexing.IndexUpdateChecker;

/**
 * Lucene Implementation of IndexUpdateChecker.
 * Walks an Index and compares Identifier/Timestamp pairs to the Objects in the Index
 * 
 * Last changed: $Date: 2009-09-02 17:57:48 +0200 (Mi, 02 Sep 2009) $
 * @version $Revision: 180 $
 * @author $Author: supnig@constantinopel.at $
 */
public class LuceneIndexUpdateChecker extends IndexUpdateChecker {

	/**
	 * 
	 */
	private LuceneIndexLocation indexLocation;

	/**
	 * 
	 */
	private IndexAccessor indexAccessor;

	/**
	 * 
	 */
	private LinkedHashMap<String, Integer> docs;

	/**
	 * 
	 */
	private Vector<String> checkedDocuments;

	/**
	 * Log4j logger.
	 */
	private static final Logger log = Logger.getLogger(LuceneIndexUpdateChecker.class);

	/**
	 * Initializes the Lucene Implementation of {@link IndexUpdateChecker}.
	 * @param indexLocation 
	 * @param termKey Key to use for storing termValue in the Index e.g. CRID
	 * @param termValue Value to use for iteration e.g. CRID_1
	 * @param idAttribute ID-Attribute key that will be used for Identifier
	 * comparison. This has to represent the field where the identifier in the
	 * method {@link com.gentics.cr.lucene.indexer.index.LuceneIndexUpdateChecker#checkUpToDate(String, Object, String, Resolvable)} 
	 * is present.
	 */
	public LuceneIndexUpdateChecker(final LuceneIndexLocation indexLocation, final String termKey, final String termValue,
		final String idAttribute) {
		this.indexLocation = indexLocation;
		indexAccessor = indexLocation.getAccessor();
		IndexReader reader = null;
		try {
			reader = indexAccessor.getReader(true);

			TermDocs termDocs = reader.termDocs(new Term(termKey, termValue));
			log.debug("Fetching sorted documents from index...");
			docs = fetchSortedDocs(termDocs, reader, idAttribute);
			log.debug("Fetched sorted docs from index");

			checkedDocuments = new Vector<String>(100);

			//TODO CONTINUE HERE PREPARE TO USE ITERATOR IN CHECK METHOD
		} catch (Throwable e) {
			log.error("Error while retrieving termdocs. Next step: close down connection in finally block", e);
		} finally {
			if (indexAccessor != null && reader != null) {
				log.debug("Closing down indexreader of IndexLocation: " + indexLocation + "/" + termValue
						+ " with write permissions (LuceneIndexUpdateChecker instantiation failed)");
				indexAccessor.release(reader, true);
			}
		}
	}

	@Override
	protected final boolean checkUpToDate(final String identifier, final Object timestamp, final String timestampattribute,
			final Resolvable object) {
		String timestampString;
		if (timestamp == null) {
			return false;
		} else {
			timestampString = timestamp.toString();
		}
		if ("".equals(timestampString)) {
			return false;
		}

		boolean readerWithWritePermissions = false;
		if (docs.containsKey(identifier)) {

			Integer documentId = docs.get(identifier);
			IndexReader reader = null;
			try {
				reader = indexAccessor.getReader(readerWithWritePermissions);
				Document document = reader.document(documentId);
				checkedDocuments.add(identifier);
				Object documentUpdateTimestamp = null;
				try {
					documentUpdateTimestamp = document.get(timestampattribute);
				} catch (NumberFormatException e) {
					log.debug("Got an error getting the document for " + identifier + " from index", e);
				}

				indexAccessor.release(reader, readerWithWritePermissions);
				//Use strings to compare the attributes
				if (documentUpdateTimestamp != null && !(documentUpdateTimestamp instanceof String)) {
					documentUpdateTimestamp = documentUpdateTimestamp.toString();
				}
				if (documentUpdateTimestamp == null || !documentUpdateTimestamp.equals(timestampString)) {
					log.debug(identifier + ": object is not up to date.");
					return false;
				}
				log.debug(identifier + ": object is up to date.");
				return true;
			} catch (IOException e) {
				//TODO specify which index is not readable
				StringBuilder directories = new StringBuilder();
				Directory[] dirs = indexLocation.getDirectories();
				for (Directory dir : dirs) {
					directories.append(dir.toString() + '\n');
				}
				log.error("Cannot open index for reading. (Directory: " + directories.toString() + ")", e);
				return true;
			} finally {
				if (indexAccessor != null) {
					indexAccessor.release(reader, readerWithWritePermissions);
					log.debug("Released reader with write permission: " + readerWithWritePermissions + " at thread: "
							+ Thread.currentThread().getName() + " - threadid: " + Thread.currentThread().getId());
				}
			}
		} else {
			//object is not yet in the index => it is not up to date
			return false;
		}
	}

	@Override
	public void deleteStaleObjects() {
		log.debug(checkedDocuments.size() + " objects checked, " + docs.size() + " objects already in the index.");
		IndexReader writeReader = null;
		boolean readerNeedsWrite = true;
		UseCase deleteStale = MonitorFactory.startUseCase("LuceneIndexUpdateChecker.deleteStaleObjects(" + indexLocation.getName() + ")");
		try {
			boolean objectsDeleted = false;
			for (String contentId : docs.keySet()) {
				if (!checkedDocuments.contains(contentId)) {
					log.debug("Object " + contentId + " wasn't checked in the last run. So i will delete it.");
					if (writeReader == null) {
						writeReader = indexAccessor.getReader(readerNeedsWrite);
					}
					writeReader.deleteDocument(docs.get(contentId));
					objectsDeleted = true;
				}
			}
			if (objectsDeleted) {
				indexLocation.createReopenFile();
			}
		} catch (IOException e) {
			log.error("Cannot delete objects from index.", e);
		} finally {
			//always release writeReader it blocks other threads if you don't 
			if (writeReader != null) {
				indexAccessor.release(writeReader, readerNeedsWrite);
			}
			log.debug("Finished cleaning stale documents");
			deleteStale.stop();
		}
		checkedDocuments.clear();
	}

	private LinkedHashMap<String, Integer> fetchSortedDocs(final TermDocs termDocs, final IndexReader reader, final String idAttribute)
			throws IOException {
		LinkedHashMap<String, Integer> tmp = new LinkedHashMap<String, Integer>();

		while (termDocs.next()) {
			Document doc = reader.document(termDocs.doc());
			String docID = doc.get(idAttribute);
			tmp.put(docID, termDocs.doc());
		}

		LinkedHashMap<String, Integer> ret = new LinkedHashMap<String, Integer>(tmp.size());
		Vector<String> v = new Vector<String>(tmp.keySet());
		Collections.sort(v);
		for (String id : v) {
			ret.put(id, tmp.get(id));
		}
		return ret;
	}

}
