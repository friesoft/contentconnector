package com.gentics.cr.lucene.indexer.index;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;

import com.gentics.cr.CRConfig;
import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.exceptions.CRException;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.util.indexing.AbstractUpdateCheckerJob;
import com.gentics.cr.util.indexing.IndexLocation;

public class CRLuceneOptimizeJob extends AbstractUpdateCheckerJob {

	/**
	 * static LOG4j {@link Logger} to LOG errors and debug.
	 */
	private static final Logger LOG = Logger.getLogger(CRLuceneOptimizeJob.class);

	public CRLuceneOptimizeJob(CRConfig config, IndexLocation indexLoc, ConcurrentHashMap<String, CRConfigUtil> configmap) {
		super(config, indexLoc, configmap);
	}

	@Override
	protected final void indexCR(final IndexLocation indexLocation, final CRConfigUtil config) throws CRException {
		if (indexLocation instanceof LuceneIndexLocation) {
			LOG.debug("Starting to optimize index.");
			LuceneIndexLocation luceneIndexLoccation = (LuceneIndexLocation) indexLocation;
			IndexAccessor ia = luceneIndexLoccation.getAccessor();
			IndexWriter writer = null;
			try {
				writer = ia.getWriter();
				writer.optimize();
			} catch (IOException e) {
				LOG.error("Optimize index.", e);
			} finally {
				ia.release(writer);
			}
			LOG.debug("Finished optimizing index.");
		} else {
			LOG.error("Index does not seem to be a Lucene index. Therfore no " + "optimizing will be done.");
		}
	}

}
