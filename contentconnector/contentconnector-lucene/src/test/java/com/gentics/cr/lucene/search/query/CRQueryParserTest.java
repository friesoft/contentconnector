package com.gentics.cr.lucene.search.query;

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
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;

import com.gentics.cr.CRRequest;
import com.gentics.cr.lucene.AbstractLuceneTest;
import com.gentics.cr.lucene.LuceneVersion;
import com.gentics.cr.lucene.search.query.mocks.ComparableDocument;
import com.gentics.cr.lucene.search.query.mocks.SimpleLucene;

public class CRQueryParserTest extends AbstractLuceneTest {

	public CRQueryParserTest(final String name) {
		super(name);
	}

	private static final Map<String, Analyzer> FIELD_ANALYZERS = new HashMap<String, Analyzer>();
	static {
		FIELD_ANALYZERS.put("categoryId", new WhitespaceAnalyzer(LuceneVersion.getVersion()));
	}

	private static final PerFieldAnalyzerWrapper STANDARD_ANALYZER = new PerFieldAnalyzerWrapper(new StandardAnalyzer(
			LuceneVersion.getVersion(),
			CharArraySet.EMPTY_SET), FIELD_ANALYZERS);
	private static final String[] SEARCHED_ATTRIBUTES = new String[] { SimpleLucene.CONTENT_ATTRIBUTE, "binarycontent" };
	private CRQueryParser parser;
	private CRRequest crRequest;
	private SimpleLucene lucene;
	private ArrayList<ComparableDocument> documents;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, true);
		crRequest = new CRRequest();
		lucene = new SimpleLucene();

		documents = new ArrayList<ComparableDocument>();
		/* 0 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word9 word1 exact",
			"node_id:1")));
		/* 1 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word2 word9 exactamente",
			"node_id:1")));
		/* 2 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word3",
			"binarycontent:word9",
			"node_id:2")));
		/* 3 */documents.add(new ComparableDocument(lucene.add(SimpleLucene.CONTENT_ATTRIBUTE
				+ ":wörd4 with-minusinit with-minus-in-it", "node_id:2")));
		/* 4 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word5 minusinit with",
			"updatetimestamp:1311604678",
			"edittimestamp:1311604678",
			"node_id:3"))); //25.07.2011 16:37:58
		/* 5 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word6",
			"updatetimestamp:1313160620",
			"edittimestamp:1313160620",
			"node_id:3"))); //12.08.2011 16:50:20
		/* 6 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":,word7, something 4,500.66",
			"updatetimestamp:1314627329",
			"edittimestamp:1314627329",
			"node_id:3"))); //29.08.2011 16:15:29
		/* 7 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":word8 01 something,different",
			"updatetimestamp:1304510397",
			"edittimestamp:1304510397",
			"node_id:3"))); //04.05.2011 13:59:57
		/* 8 */documents.add(new ComparableDocument(lucene.add(
			SimpleLucene.CONTENT_ATTRIBUTE + ":newword 01/23456789",
				"node_id:11")));
		/* 9 */documents.add(new ComparableDocument(lucene.add("categoryId:category-with-minus", SimpleLucene.CONTENT_ATTRIBUTE
				+ ":content")));

	}

	public void testReplaceBooleanMnoGoSearchQuery() throws ParseException, CorruptIndexException, IOException {

		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word1 | word2")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("word1 & word9")));
		containsOnly(matchedDocuments, documents.get(0));

		matchedDocuments = wrapComparable(lucene.find(parser.parse("word1&word9")));
		containsOnly(matchedDocuments, documents.get(0));
	}

	public void testLogicalOperator() throws ParseException, CorruptIndexException, IOException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word1 OR word3")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(2) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("word1 AND word9")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("word9 OR word1")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1), documents.get(2) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("word9 NOT word1")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(1), documents.get(2) });
	}

	public void testManualUseOfWildcard() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "beg");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		parser.setAllowLeadingWildcard(true);

		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("*act AND word")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0) });

	}

	public void testExactSearch() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "beg");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);

		//		System.out.println("parsed query: " + parser.parse("exact").toString());
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("exact")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("'exact'")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0) });

	}


	public void testSearchAttributes() throws ParseException, CorruptIndexException, IOException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word1")));
		containsOnly(matchedDocuments, documents.get(0));
	}

	public void testCategoryAttribute() throws CorruptIndexException, IOException, ParseException {
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("categoryId:category\\-with\\-minus")));
		containsOnly(matchedDocuments, documents.get(9));
	}

	public void testWordMatchSub() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1), documents.get(2),
				documents.get(4), documents.get(5), documents.get(6), documents.get(7), documents.get(8) });
	}
	
	
	public void testWordMatchWithCommaAtEnd() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "beg");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word7, AND something")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}
	
	public void testWordMatchWithCommaAtBegin() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "end");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse(",word7 AND something")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}
	
	public void testWordMatchWithCommaAtBeginAndEnd() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse(",word7, AND something")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}

	public void testWordMatchBeg() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "beg");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1), documents.get(2),
				documents.get(4), documents.get(5), documents.get(6), documents.get(7) });
	}

	public void testWordMatchEnd() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "end");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word")));
		containsOnly(matchedDocuments, documents.get(8));
	}

	public void testWordMatchWrd() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "wrd");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<Document> matchedDocuments = lucene.find(parser.parse("word"));
		assertTrue(matchedDocuments.size() == 0);
	}

	public void testWordMatchComplexSub() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("word AND node_id:1")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1) });
	}

	public void testWordMatchComplexSubMultipleTerms() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("content:(word1 word9) AND node_id:1")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1) });
	}

	public void testWordMatchComplexSubGroupMultipleTerms() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("content:\"word1 word9\" AND node_id:1")));
		assertTrue(matchedDocuments.size() == 0);
		matchedDocuments = wrapComparable(lucene.find(parser.parse("content:\"word9 word1\" AND node_id:1")));
		containsOnly(matchedDocuments, documents.get(0));
	}

	public void testWordMatchComplexSubRangeOperator() throws ParseException, CorruptIndexException, IOException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser
				.parse("content:word AND edittimestamp:[1304510397 TO 1314627329]")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(4), documents.get(5), documents.get(6),
				documents.get(7) });

		crRequest.set(CRRequest.WORDMATCH_KEY, "wrd");
		parser = new CRQueryParser(LuceneVersion.getVersion(), new String[] { "edittimestamp" }, STANDARD_ANALYZER,
				crRequest);
		matchedDocuments = wrapComparable(lucene.find(parser.parse("[1304510397 TO 1314627329]")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(4), documents.get(5), documents.get(6),
				documents.get(7) });

		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), new String[] { "edittimestamp" }, STANDARD_ANALYZER,
				crRequest);
		matchedDocuments = wrapComparable(lucene.find(parser.parse("[1304510397 TO 1314627329]")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(4), documents.get(5), documents.get(6),
				documents.get(7) });
	}

	public void testWordMatchComplexSubGroupAddSign() throws CorruptIndexException, IOException, ParseException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("+(word1 word9)")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(0), documents.get(1), documents.get(2) });
	}

	public void testMinus() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("with\\-minusinit")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(3), documents.get(4) });
		matchedDocuments = wrapComparable(lucene.find(parser.parse("with-minusinit")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(3), documents.get(4) });
	}

	public void testMultipleMinus() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("with-minus-in-it")));
		containsOnly(matchedDocuments, documents.get(3));
	}

	public void testMultipleEscapedMinus() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("with\\-minus\\-in\\-it")));
		containsOnly(matchedDocuments, documents.get(3));
	}

	public void testEscapedMinusWordMatch() throws CorruptIndexException, IOException, ParseException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("with\\-minusinit")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(3), documents.get(4) });
	}

	public void testEscapedMinusAndPrefixedSpecialCharactersWordMatch() throws CorruptIndexException, IOException, ParseException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("\\-with\\-minusinit")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(3), documents.get(4) });
	}

	public void testNumberWithSlashes() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("01/23456789")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(8) });
	}
	
	public void testNumberWithSlashesAndWildcard() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("01/2*")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(8) });
	}
	
	public void testWordWithComma() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("something,different")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(7) });

		matchedDocuments = wrapComparable(lucene.find(parser.parse("something, different")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(7), documents.get(6) });
	}

	public void testWordWithClutter() throws CorruptIndexException, IOException, ParseException {
		parser.setAllowLeadingWildcard(true);
		//		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("+-/-something!\"§$%&/()=?different"))); // () not allowed
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("+-/-something!\"§$%&/=?different")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(7) });
	}

	public void testNumberWithSlashesAndWildcards() throws CorruptIndexException, IOException, ParseException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("01/23456789")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(8) });
		matchedDocuments = wrapComparable(lucene.find(parser.parse("01/23")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(8) });
	}
	
	public void testNumberWithComma() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("4,500.66")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}

	public void testNumberWithCommaAndWildcard() throws CorruptIndexException, IOException, ParseException {
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("4,500*")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}

	public void testNumberWithCommaAndWildcards() throws CorruptIndexException, IOException, ParseException {
		crRequest.set(CRRequest.WORDMATCH_KEY, "sub");
		parser = new CRQueryParser(LuceneVersion.getVersion(), SEARCHED_ATTRIBUTES, STANDARD_ANALYZER, crRequest);
		Collection<ComparableDocument> matchedDocuments = wrapComparable(lucene.find(parser.parse("4,5")));
		containsAll(matchedDocuments, new ComparableDocument[] { documents.get(6) });
	}
}
