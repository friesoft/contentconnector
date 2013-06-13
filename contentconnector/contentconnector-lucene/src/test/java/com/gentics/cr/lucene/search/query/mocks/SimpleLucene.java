package com.gentics.cr.lucene.search.query.mocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import com.gentics.cr.lucene.LuceneVersion;

public class SimpleLucene {

	public static final String CONTENT_ATTRIBUTE = "content";

	
	private static final Map<String, Analyzer> FIELD_ANALYZERS = new HashMap<String, Analyzer>();
	static {
		FIELD_ANALYZERS.put("categoryId", new WhitespaceAnalyzer(LuceneVersion.getVersion()));
	}
	
	private static final PerFieldAnalyzerWrapper ANALYZER = new PerFieldAnalyzerWrapper(new StandardAnalyzer(LuceneVersion.getVersion(),
			CharArraySet.EMPTY_SET), FIELD_ANALYZERS);

	IndexSearcher searcher;

	RAMDirectory index;

	public SimpleLucene() throws CorruptIndexException, IOException {
		index = new RAMDirectory();
	}

	public IndexReader getReader() throws CorruptIndexException, IOException {
		return IndexReader.open(index);
	}

	public void add(final Document document) throws CorruptIndexException, IOException {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LuceneVersion.getVersion(), ANALYZER);
		IndexWriter writer = new IndexWriter(index, indexWriterConfig);
		writer.addDocument(document);
		writer.optimize();
		writer.close();
	}

	public Document add(final String text) throws CorruptIndexException, IOException {
		Document document = new Document();
		document.add(new Field(SimpleLucene.CONTENT_ATTRIBUTE, text, Field.Store.YES, Field.Index.ANALYZED,
				TermVector.WITH_POSITIONS_OFFSETS));
		add(document);
		return document;
	}

	public Document add(final Map<String, String> fields) throws CorruptIndexException, IOException {
		Document document = new Document();
		for (String fieldName : fields.keySet()) {
			document.add(new Field(fieldName, fields.get(fieldName), Field.Store.YES, Field.Index.ANALYZED,
					TermVector.WITH_POSITIONS_OFFSETS));
		}
		add(document);
		return document;
	}

	public Document add(final String... fields) throws CorruptIndexException, IOException {
		Document document = new Document();
		for (String field : fields) {
			String name = field.replaceAll(":.*", "");
			String value = field.substring(name.length() + 1);
			document.add(new Field(name, value, Field.Store.YES, Field.Index.ANALYZED,
					TermVector.WITH_POSITIONS_OFFSETS));
		}
		add(document);
		return document;
	}

	public Collection<Document> find(final String luceneQuery) throws ParseException, IOException {
		QueryParser queryParser = new QueryParser(LuceneVersion.getVersion(), CONTENT_ATTRIBUTE, ANALYZER);
		Query query = queryParser.parse(luceneQuery);
		return find(query);
	}

	public Collection<Document> find(final Query query) throws CorruptIndexException, IOException {
		initSearcher();
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		ArrayList<Document> result = new ArrayList<Document>();
		for (ScoreDoc document : hits.scoreDocs) {
			result.add(searcher.doc(document.doc));
		}
		return result;
	}

	private void initSearcher() throws CorruptIndexException, IOException {
		if (searcher == null) {
			searcher = new IndexSearcher(index);
		}
	}

	public IndexSearcher getSearcher() throws CorruptIndexException, IOException {
		initSearcher();
		return searcher;
	}

}
