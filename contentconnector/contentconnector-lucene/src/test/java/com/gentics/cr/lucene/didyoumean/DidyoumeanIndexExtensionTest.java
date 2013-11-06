package com.gentics.cr.lucene.didyoumean;

import java.util.Date;

import org.apache.lucene.store.Directory;
import org.junit.Test;

import com.gentics.cr.CRConfig;
import com.gentics.cr.configuration.GenericConfiguration;
import com.gentics.cr.configuration.SimpleCRConfig;
import com.gentics.cr.lucene.facets.taxonomy.taxonomyaccessor.TaxonomyAccessor;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.lucene.indexer.index.LuceneIndexLocation;
import com.gentics.cr.util.indexing.IndexLocation;

public class DidyoumeanIndexExtensionTest {
	
	private final class IndexLocationMock extends LuceneIndexLocation {
		private IndexLocationMock(final CRConfig givenConfig) {
			super(givenConfig);
		}

		@Override
		public boolean isLocked() {
			return false;
		}

		@Override
		public void createReopenFile() {
		}

		@Override
		protected Directory[] getDirectories() {
			return null;
		}

		@Override
		public int getDocCount() {
			return 0;
		}

		@Override
		protected IndexAccessor getAccessorInstance() {
			return null;
		}

		@Override
		protected IndexAccessor getAccessorInstance(final boolean reopenClosedFactory) {
			return null;
		}

		@Override
		public boolean reopenCheck(final IndexAccessor indexAccessor, final TaxonomyAccessor taxonomyAccessor) {
			return false;
		}

		@Override
		public Date lastModified() {
			return null;
		}

		@Override
		public long indexSize() {
			return 0;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		protected TaxonomyAccessor getTaxonomyAccessorInstance() {
			return null;
		}
	}


	@Test
	public void testNoAlreadyClosedExceptionOnSecondClose() {
		CRConfig config = new SimpleCRConfig();
		GenericConfiguration subconfig = new GenericConfiguration();
		subconfig.set("indexLocationClass", "com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation");
		subconfig.set("indexLocations.0.path", "/dummy/");
		config.setSubConfig("didyoumeanlocation", subconfig);
		IndexLocation callingLocation = new IndexLocationMock(config);
		DidyoumeanIndexExtension die = new DidyoumeanIndexExtension(config, callingLocation);

		die.stop();
		die.stop();
	}

}
