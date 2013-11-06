package org.apache.lucene.search.spell;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import com.gentics.cr.CRConfig;
import com.gentics.cr.configuration.SimpleCRConfig;
import com.gentics.cr.lucene.facets.taxonomy.taxonomyaccessor.TaxonomyAccessor;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.lucene.indexer.index.LuceneIndexLocation;

public class CustomSpellCheckerTest {

	private final class LuceneIndexLocationMock extends LuceneIndexLocation {
		private LuceneIndexLocationMock(final CRConfig config) {
			super(config);
		}

		@Override
		public boolean isLocked() {
			return false;
		}

		@Override
		public void createReopenFile() {
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

		@Override
		public int getDocCount() {
			return 0;
		}

		@Override
		protected Directory[] getDirectories() {
			return null;
		}

		@Override
		protected IndexAccessor getAccessorInstance(final boolean reopenClosedFactory) {
			return null;
		}

		@Override
		protected IndexAccessor getAccessorInstance() {
			return null;
		}
	}

	@Test(expected = AlreadyClosedException.class)
	public void testAlreadyClosedException() throws IOException {
		CRConfig config = new SimpleCRConfig();
		LuceneIndexLocation sspellIndex = new LuceneIndexLocationMock(config);
		StringDistance xsd = new LevensteinDistance();
		CustomSpellChecker csc = new CustomSpellChecker(sspellIndex, xsd);

		csc.close();
		csc.close();
	}

}
