package com.gentics.cr.lucene.indexer.transformer.lang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.gentics.cr.CRResolvableBean;
import com.gentics.cr.analytics.language.LanguageGuesser;
import com.gentics.cr.configuration.GenericConfiguration;
import com.gentics.cr.lucene.indexer.transformer.ContentTransformer;

/**
 * 
 * Last changed: $Date: 2009-09-02 17:57:48 +0200 (Mi, 02 Sep 2009) $
 * @version $Revision: 180 $
 * @author $Author: supnig@constantinopel.at $
 *
 */
public class LanguageIdentifyer extends ContentTransformer {

	private static final String TRANSFORMER_ATTRIBUTE_KEY = "attribute";
	private static final String LANGUAGE_ATTRIBUTE_KEY = "langattribute";
	private String attribute;
	private String langattribute;

	/**
	 * Language used if no language is found. Defaults to 'NULL'
	 */
	private String defaultLanguage;

	/**
	 * List of allowed languaged. If found language is not in list, then the default language is taken.
	 */
	private List<String> allowedLanguages = null;

	/**
	 * Create new instance of LanguageIdentifyer
	 * @param config
	 */
	public LanguageIdentifyer(final GenericConfiguration config) {
		super(config);
		attribute = (String) config.get(TRANSFORMER_ATTRIBUTE_KEY);
		langattribute = (String) config.get(LANGUAGE_ATTRIBUTE_KEY);
		defaultLanguage = (String) config.get("defaultlanguage");
		if (StringUtils.isEmpty(defaultLanguage)) {
			defaultLanguage = "NULL";
		}
		if (StringUtils.isNotEmpty((String) config.get("allowedlanguages"))) {
			allowedLanguages = Arrays.asList(((String) config.get("allowedlanguages")).split(","));
		}
	}

	@Override
	public void processBean(final CRResolvableBean bean) {
		Object att = bean.get(attribute);
		if (att != null && langattribute != null) {
			String lang = null;
			if (att instanceof String) {
				lang = getLangFromString((String) att);
			} else if (att instanceof byte[]) {
				lang = getLangFromBinary((byte[]) att);
			}
			if (lang == null) {
				lang = defaultLanguage;
			}
			if (allowedLanguages != null) {
				if (!lang.equals(defaultLanguage) && !allowedLanguages.contains(lang)) {
					lang = defaultLanguage;
				}
			}
			bean.set(langattribute, lang);
		}
	}

	private String getLangFromBinary(final byte[] binary) {
		String lang = null;
		ByteArrayInputStream is = new ByteArrayInputStream(binary);
		try {
			lang = LanguageGuesser.detectLanguage(is);
		} catch (IOException iox) {
			iox.printStackTrace();
		}
		return lang;
	}

	private String getLangFromString(final String string) {
		return LanguageGuesser.detectLanguage(string);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
