package com.gentics.cr.lucene.search;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.CRRequest;
import com.gentics.cr.CRResolvableBean;
import com.gentics.cr.exceptions.CRException;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.lucene.indexer.index.LuceneIndexLocation;
import com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation;
import com.gentics.cr.lucene.search.highlight.ContentHighlighter;
import com.gentics.cr.lucene.search.highlight.PhraseBolder;
import com.gentics.cr.lucene.search.highlight.VectorBolder;
import com.gentics.cr.util.indexing.IndexLocation;

public class LuceneRequestProcessorTest {
	
	private int ids = 0;
	
	public static final String indexLocationPath = LuceneRequestProcessorTest.class.getResource(".").getPath() + File.separator + "index";
	private LuceneRequestProcessor rp;
	private CRConfigUtil config;
	
	
	public void setupConfig() throws CRException {
		config = new CRConfigUtil();
		config.setName("testIndexConfig");
		config.set("searchcount", "10");
		config.set(IndexLocation.INDEX_LOCATION_CLASS_KEY, LuceneSingleIndexLocation.class.getName());
		config.set(LuceneRequestProcessor.SEARCHED_ATTRIBUTES_KEY, "content");
		config.set(LuceneRequestProcessor.ID_ATTRIBUTE_KEY, "id");
		config.set(LuceneRequestProcessor.GET_STORED_ATTRIBUTE_KEY, "true");
		//index location configs
		CRConfigUtil indexLocationConfigs = new CRConfigUtil();
		config.setSubConfig(IndexLocation.INDEX_LOCATIONS_KEY, indexLocationConfigs);
		CRConfigUtil indexLocationConfig = new CRConfigUtil();
		indexLocationConfig.set(IndexLocation.INDEX_PATH_KEY, indexLocationPath);
		indexLocationConfigs.setSubConfig("1", indexLocationConfig);
		
		//highligher configs
		CRConfigUtil highlighterConfig = new CRConfigUtil();
		config.setSubConfig(ContentHighlighter.HIGHLIGHTER_KEY, highlighterConfig);
		CRConfigUtil contentHighlighterConfig = new CRConfigUtil();
		highlighterConfig.setSubConfig("1", contentHighlighterConfig);
		contentHighlighterConfig.set(ContentHighlighter.HIGHLIGHTER_ATTRIBUTE_KEY, "content");
		//vector bolder generates the field query himself (thourgh highlighter)
		//contentHighlighterConfig.set(ContentHighlighter.HIGHLIGHTER_CLASS_KEY, VectorBolder.class.getName());
		//test if the phrase bolder only highlights terms matching the field it highlights (e.g. not the 12 in the content) 
		contentHighlighterConfig.set(ContentHighlighter.HIGHLIGHTER_CLASS_KEY, PhraseBolder.class.getName());
		contentHighlighterConfig.set(ContentHighlighter.HIGHLIGHTER_RULE_KEY, "1==1");
		contentHighlighterConfig.set(VectorBolder.PHRASE_PREFIX_KEY, "<strong>");
		contentHighlighterConfig.set(VectorBolder.PHRASE_POSTFIX_KEY, "</strong>");
		
		
		rp = new LuceneRequestProcessor(config);
	}
	
	@Before
	public void initIndex() throws IOException, CRException {
		setupConfig();
		LuceneIndexLocation location = LuceneIndexLocation.getIndexLocation(config);
		IndexAccessor accessor = location.getAccessor();
		IndexWriter writer = accessor.getWriter();
		writer.addDocument(createDocument("inhalt der die contentid enthält 12", "12"));
		writer.commit();
		accessor.release(writer);
		location.createReopenFile();
	}
	
	@After
	public void clearIndex() throws CRException, IOException {
		setupConfig();
		LuceneIndexLocation location = LuceneIndexLocation.getIndexLocation(config);
		IndexAccessor accessor = location.getAccessor();
		IndexWriter writer = accessor.getWriter();
		writer.deleteAll();
		writer.commit();
		accessor.release(writer);
		location.createReopenFile();
	}
	
	private Document createDocument(String content, String node) {
		Document doc = new Document();
		doc.add(new Field("content", content, Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("node", node, Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("id", ids++ + "", Store.YES, Index.NO));
		return doc;
	}

	@Test
	public void testHighlightingDifferentAttributes() throws CRException {
		CRRequest request = new CRRequest("content:inhalt +node:12");
		Collection<CRResolvableBean> result = rp.getObjects(request);
		assertNotNull("Didn't get a result. Is something wrong with the lucene config?", result);
		assertEquals(1, result.size());
		assertEquals("<strong>inhalt</strong> der die contentid enthält 12", result.iterator().next().get("content"));
		
		
	}
}
