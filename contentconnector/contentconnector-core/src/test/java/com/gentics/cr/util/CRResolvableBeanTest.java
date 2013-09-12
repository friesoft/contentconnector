package com.gentics.cr.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.Charset;

import org.junit.Test;

import com.gentics.cr.CRResolvableBean;

public class CRResolvableBeanTest {

	@Test
	public void testContentidConstructor() {
		CRResolvableBean bean = new CRResolvableBean("3.4");
		assertEquals("Contentid not correct.", "3.4", bean.getContentid());
		assertEquals("obj_id not correct.", "4", bean.getObj_id());
		assertEquals("obj_type not correct.", "3", bean.getObj_type());
		assertEquals("obj_id via default get not correct.", "4", bean.get("obj_id"));
		assertEquals("obj_type via default get not correct.", "3", bean.get("obj_type"));
	}
	
	@Test
	public void testGetContent() {
		CRResolvableBean bean = new CRResolvableBean();
		bean.set("content", "String");
		assertEquals("String", bean.getContent());
		
		bean.set("content", "bytes".getBytes(Charset.forName("UTF-8")));
		assertEquals("bytes", bean.getContent());
		
		bean.set("content", new StringBuilder().append("StringBuilder"));
		assertEquals("StringBuilder", bean.getContent());
	}
	
	@Test
	public void testGetContentWithEncoding() {
		final Charset UTF8 = Charset.forName("UTF-8");
		final Charset ISO885915 = Charset.forName("ISO-8859-15");
		final String UMLAUTS = "String with Umlauts ÖÄÜöüäß€";
		
		CRResolvableBean bean = new CRResolvableBean();
		bean.set("content", UMLAUTS.getBytes(UTF8));
		
		
		assertEquals(UMLAUTS, bean.getContent("UTF-8"));
		assertEquals("Default encoding should be UTF-8", UMLAUTS, bean.getContent());
		
		assertNotNull("Your environment doesn't support ISO-8859-15", ISO885915);
		assertEquals("Encoding in iso doesn't work (there are more characters than the iso string would have).",
				UMLAUTS.length(), UMLAUTS.getBytes(ISO885915).length);
		bean.set("content", UMLAUTS.getBytes(ISO885915));
		assertEquals(new String(UMLAUTS.getBytes(ISO885915), ISO885915), bean.getContent("ISO-8859-15"));
	}
	
	@Test
	public void testGetContentStringBuilderWithEncoding() {
		final Charset UTF8 = Charset.forName("UTF-8");
		final Charset ISO885915 = Charset.forName("ISO-8859-15");
		final String UMLAUTS = "String with Umlauts ÖÄÜöüäß€";
		
		CRResolvableBean bean = new CRResolvableBean();
		bean.set("content", new StringBuilder().append(new String(UMLAUTS.getBytes(UTF8))));
		
		
		assertEquals(UMLAUTS, bean.getContent("UTF-8"));
		assertEquals("Default encoding should be UTF-8", UMLAUTS, bean.getContent());
		
		assertNotNull("Your environment doesn't support ISO-8859-15", ISO885915);
		assertEquals("Encoding in iso doesn't work (there are more characters than the iso string would have).",
				UMLAUTS.length(), UMLAUTS.getBytes(ISO885915).length);
		bean.set("content", new StringBuilder().append(new String(UMLAUTS.getBytes(ISO885915), ISO885915)));
		assertEquals(new String(UMLAUTS.getBytes(ISO885915), ISO885915), bean.getContent("ISO-8859-15"));
	
	}
	
	@Test
	public void testFormat() {
		CRResolvableBean bean = new CRResolvableBean("10007.8");
		bean.set("name", "Testname");
		assertEquals("10007.8", bean.format());

		assertEquals("Testname: 10007.8", bean.format("%(name): %(contentid)"));

		assertEquals("null: 10007.8", bean.format("%(asdf): %(contentid)"));

		bean.set("myId", "10002");
		assertEquals("10002: Testname", bean.format("myId", "%(idAttribute): %(name)"));

		CRResolvableBean bean2 = new CRResolvableBean();
		assertEquals("10001", bean2.format());
	}

}
