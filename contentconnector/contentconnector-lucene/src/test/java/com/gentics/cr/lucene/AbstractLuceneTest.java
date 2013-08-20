package com.gentics.cr.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.gentics.cr.lucene.search.query.mocks.ComparableDocument;
import com.gentics.cr.lucene.search.query.mocks.SimpleLucene;

public abstract class AbstractLuceneTest extends TestCase {

	private Collection<ComparableDocument> documents;
	private Document document1;
	private Document document3;
	private Document document2;
	
	private ComparableDocument cd1;
	private ComparableDocument cd2;
	private ComparableDocument cd3;

	public AbstractLuceneTest(final String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		document1 = new Document();
		document1.add(new Field(SimpleLucene.CONTENT_ATTRIBUTE, "document1", Field.Store.YES, Field.Index.ANALYZED));
		cd1 = new ComparableDocument(document1);
		document2 = new Document();
		document2.add(new Field(SimpleLucene.CONTENT_ATTRIBUTE, "document2", Field.Store.YES, Field.Index.ANALYZED));
		cd2 = new ComparableDocument(document2);
		document3 = new Document();
		document3.add(new Field(SimpleLucene.CONTENT_ATTRIBUTE, "document3", Field.Store.YES, Field.Index.ANALYZED));
		cd3 = new ComparableDocument(document3);

		documents = new ArrayList<ComparableDocument>();
		documents.add(cd1);
		documents.add(cd2);

	}

	public void selfTest() {
		containsAll(documents, new ComparableDocument[] { new ComparableDocument(document1),
				new ComparableDocument(document2), new ComparableDocument(document3) });
		documents.remove(cd2);
		documents.remove(cd3);
		containsOnly(documents, new ComparableDocument(document1));
	}

	protected void containsAll(final Collection<ComparableDocument> matchedDocuments, final ComparableDocument[] documents) {
		assertEquals("Wrong size of matched documents.", documents.length, matchedDocuments.size());
		assertTrue("Wrong result items.", matchedDocuments.containsAll(Arrays.asList(documents)));
	}

	protected void containsOnly(final Collection<ComparableDocument> matchedDocuments, final ComparableDocument containedDocument) {
		assertEquals("Wrong size of matched documents.", 1, matchedDocuments.size());
		assertTrue("Wrong result item.", matchedDocuments.contains(containedDocument));
	}
	
	public Collection<ComparableDocument> wrapComparable(final Collection<Document> docColl) {
		Collection<ComparableDocument> ret = new Vector<ComparableDocument>();
		for (Document d : docColl) {
			ret.add(new ComparableDocument(d));
		}
		return ret;
	}
}
