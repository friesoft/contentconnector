package com.gentics.cr.lucene.search.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.gentics.cr.CRRequest;

/**
 * CRQueryParser allows you to use mnoGoSearch Queries and searching in
 * multiple attributes.
 * @author perhab
 */
public class CRQueryParser extends QueryParser {

	/**
	 * Constant 1.
	 */
	private static final int ONE = 1;
	/**
	 * Constant 2.
	 */
	private static final int TWO = 2;
	/**
	 * Constant 3.
	 */
	private static final int THREE = 3;
	
	/**
	 * characters that cannot be search because they are removed (replaced by a space) by the analyser before writing to the index.
	 * special characters are for now  , - / \
	 * <br>do not add wildcard symbols here as that would remove them from the query in the default attributes
	 */
	private static final String SPECIAL_CHARACTERS = ",-\\/\\\\&";

	/**
	 * Special characters that also separate numbers. The analyzer doesn't separate numbers with , or . in them.
	 */
	private static final String SPECIAL_CHARACTER_IN_NUMBERS = "-\\/\\\\";

	/**
	 * attributes to search in.
	 */
	private Collection<String> attributesToSearchIn;

	/**
	 * request to search for additional parameters.
	 */
	private CRRequest request;

	/**
	 * Log4j logger for error and debug messages.
	 */
	private static final Logger LOGGER = Logger.getLogger(CRQueryParser.class);


	private static final String seperatorCharacterClass = " \\(\\)";

	/** Matches an attribute value pair. */
	private static final Pattern PATTERN_ATTRIBUTE_VALUE = Pattern.compile("([" + seperatorCharacterClass + "]*)"
			+ "([^:]+:(?:\\([^\\)]+\\)|\\[[^\\]]+\\]|\"[^\"]+\")|\"[^\"]+\"|[^" + seperatorCharacterClass + "]+)" + "(["
			+ seperatorCharacterClass + "]*)");
	/** Matches attribute value pair where exact search with quotes was used or were a wildcard was used. */
	private static final Pattern PATTERN_WILDCARD_OR_EXACT_SEARCH = Pattern.compile("[^:]+:(\"[^\"]+\"|.*\\*.*)");

	/** Used for appending the wordmatch wildcards. */
	private static final Pattern PATTERN_WILDCARD_APPENDER = Pattern.compile("(.*:\\(?)?([^: \\(\\)]+)");

	/**
	 * initialize a CRQeryParser with multiple search attributes.
	 * @param version version of lucene
	 * @param searchedAttributes attributes to search in
	 * @param analyzer analyzer for index
	 */
	public CRQueryParser(final Version version, final String[] searchedAttributes, final Analyzer analyzer) {
		super(version, searchedAttributes[0], analyzer);
		attributesToSearchIn = Arrays.asList(searchedAttributes);
	}

	/**
	 * initialize a CRQeryParser with multiple search attributes.
	 * @param version version of lucene
	 * @param searchedAttributes attributes to search in
	 * @param analyzer analyzer for index
	 * @param crRequest request to get additional parameters from.
	 */
	public CRQueryParser(final Version version, final String[] searchedAttributes, final Analyzer analyzer, final CRRequest crRequest) {
		this(version, searchedAttributes, analyzer);
		this.request = crRequest;
	}

	/**
	 * parse the query for lucene.
	 * @param query as {@link String}
	 * @return parsed lucene query
	 * @throws ParseException when the query cannot be successfully parsed
	 */
	@Override
	public Query parse(final String query) throws ParseException {
		String crQuery = query;
		LOGGER.debug("parsing query: " + crQuery);
		crQuery = replaceBooleanMnoGoSearchQuery(crQuery);
		if (attributesToSearchIn.size() > ONE) {
			crQuery = addMultipleSearchedAttributes(crQuery);
		}
		crQuery = addWildcardsForWordmatchParameter(crQuery);
		crQuery = replaceSpecialCharactersFromQuery(crQuery);
		LOGGER.debug("parsed query: " + crQuery);
		return super.parse(crQuery);
	}

	/**
	 * the query is splitted and all special characters are replaced in a way as
	 * if they where spaces before. So "content:a-b" becomes "content:a +content:b".
	 * @param crQuery - query to search for special characters
	 * @return query with replaced special characters
	 */
	protected String replaceSpecialCharactersFromQuery(final String crQuery) {
		StringBuffer newQuery = new StringBuffer();
		Matcher valueMatcher = getValueMatcher(crQuery);
		while (valueMatcher.find()) {
			String charsBeforeValue = valueMatcher.group(ONE);
			String valueWithAttribute = valueMatcher.group(TWO);
			String attribute = valueWithAttribute.indexOf(":") != -1 ? valueWithAttribute.replaceAll(":.*$", "") : "";
			String charsAfterValue = valueMatcher.group(THREE);
			if (!"AND".equalsIgnoreCase(valueWithAttribute) && !"OR".equalsIgnoreCase(valueWithAttribute)
					&& !"NOT".equalsIgnoreCase(valueWithAttribute) && attributesToSearchIn.contains(attribute)) {
				if (!valueWithAttribute.matches("[^:]+:\"[^\"]+\"") && valueWithAttribute.matches(".*[" + SPECIAL_CHARACTERS + "].*")) {
					String replacement = replaceSpecialCharactersInAttribute(charsBeforeValue, valueWithAttribute, attribute, charsAfterValue);
					valueMatcher.appendReplacement(newQuery, Matcher.quoteReplacement(replacement));
				}
			}
		}
		valueMatcher.appendTail(newQuery);
		return newQuery.toString();
	}

	/**
	 * helper method to replace {@link #SPECIAL_CHARACTERS} in attribute and correcting for wildcards
	 * @param charsBeforeValue - string before the value string (nothing replaced in here)
	 * @param valueWithAttribute - value and attributename in the form attribute:value
	 * @param attribute - attributename for easier replacing
	 * @param charsAfterValue - string after the value (nothing replaced here)
	 * @return charsBeforeValue + replaced valueWithAttribute + charsAfterValue
	 */
	protected String replaceSpecialCharactersInAttribute(final String charsBeforeValue,
			final String valueWithAttribute, final String attribute, final String charsAfterValue) {

		//remove special characters immediatly after or before wildcards as they would result in queries like attribute:query +attribute:*
		String cleanedValueWithAttribute = valueWithAttribute.replaceAll("[" + SPECIAL_CHARACTERS + "]+(\\*)", "$1");
		cleanedValueWithAttribute = cleanedValueWithAttribute.replaceAll("(" + attribute + ":\\*)[" + SPECIAL_CHARACTERS + "]+", "$1");
		
		//replace content:s-train with content:s +content:train as the special characters cannot be searched in attributes indexed by the StandardAnalyzer
		return charsBeforeValue + "("
				+ cleanedValueWithAttribute.replaceAll("\\\\?([^0-9][" + SPECIAL_CHARACTERS + "]|[0-9][" + SPECIAL_CHARACTER_IN_NUMBERS
						+ "])([^" + SPECIAL_CHARACTERS + "]+)", " +" + attribute + ":$2") + ")" + charsAfterValue;
	}

	/**
	 * parse given query and prepare it to search in multiple attributes with
	 * lucene, only words are replaced that are not one of AND, OR, NOT and do not
	 * contain a ":" char.
	 * @param query String with query to parse
	 * @return parsed query, in case no searchedAttributes are given the original
	 * query is given back.
	 */
	protected String addMultipleSearchedAttributes(final String query) {
		StringBuffer newQuery = new StringBuffer();
		StringBuilder replacement = new StringBuilder();
		for (String attribute : attributesToSearchIn) {
			if (replacement.length() > 0) {
				replacement.append(" OR ");
			}
			replacement.append(attribute + ":$2");
		}
		if (replacement.length() > 0) {
			replacement.insert(0, "(");
			replacement.append(")");
			Matcher valueMatcher = getValueMatcher(query);
			while (valueMatcher.find()) {
				String charsBeforeValue = valueMatcher.group(ONE);
				String value = valueMatcher.group(TWO);
				String charsAfterValue = valueMatcher.group(THREE);
				if (!"AND".equalsIgnoreCase(value) && !"OR".equalsIgnoreCase(value) && !"NOT".equalsIgnoreCase(value)
						&& !"TO".equalsIgnoreCase(value) && !"+".equals(value) && !value.contains(":")) {
					valueMatcher.appendReplacement(newQuery, charsBeforeValue + replacement.toString() + charsAfterValue);
				}
			}
			valueMatcher.appendTail(newQuery);
		}
		return newQuery.toString();
	}

	/**
	 * if the wordmatch parameter is give in the crRequest this method adds
	 * wildcards to the words in the queries.
	 * @param crQuery query to replace the words in it
	 * @return query with wildcards in it
	 */
	protected String addWildcardsForWordmatchParameter(final String crQuery) {
		String wordmatch = null;
		if (request != null) {
			wordmatch = (String) request.get(CRRequest.WORDMATCH_KEY);
		}
		String appendToWordEnd = "";
		String appendToWordBegin = "";

		if (wordmatch == null || "wrd".equalsIgnoreCase(wordmatch)) {
			return crQuery;
		} else if ("end".equalsIgnoreCase(wordmatch)) {
			super.setAllowLeadingWildcard(true);
			appendToWordBegin = "*";
		} else if ("beg".equalsIgnoreCase(wordmatch)) {
			appendToWordEnd = "*";
		} else if ("sub".equalsIgnoreCase(wordmatch)) {
			super.setAllowLeadingWildcard(true);
			appendToWordBegin = "*";
			appendToWordEnd = "*";
		}
		StringBuffer newQuery = new StringBuffer();
		Matcher valueMatcher = getValueMatcher(crQuery);
		while (valueMatcher.find()) {
			String charsBeforeValue = valueMatcher.group(ONE);
			String value = valueMatcher.group(TWO);
			String attribute = value.indexOf(":") != -1 ? value.replaceAll(":.*$", "") : "";
			String charsAfterValue = valueMatcher.group(THREE);
			if (!"AND".equalsIgnoreCase(value) && !"OR".equalsIgnoreCase(value) && !"NOT".equalsIgnoreCase(value)
					&& attributesToSearchIn.contains(attribute)) {
				// only add wildcard if no wildcard is in term and we do not use exact search (term in quotes)
				if (!PATTERN_WILDCARD_OR_EXACT_SEARCH.matcher(value).matches()) {
					String replacement = Matcher.quoteReplacement(charsBeforeValue
							+ PATTERN_WILDCARD_APPENDER.matcher(value).replaceAll("$1" + appendToWordBegin + "$2" + appendToWordEnd)
							+ charsAfterValue);
					valueMatcher.appendReplacement(newQuery, replacement);
				}
			}
		}
		valueMatcher.appendTail(newQuery);
		return newQuery.toString();
	}

	/**
	 * get Value Matcher.
	 * @param query query.
	 * @return matcher.
	 */
	protected Matcher getValueMatcher(final String query) {
		Matcher valueMatcher = PATTERN_ATTRIBUTE_VALUE.matcher(query);
		return valueMatcher;
	}

	/**
	 * Helper method to replace search parameters from boolean mnoGoSearch query
	 * into their lucene compatible parameters.
	 * @param mnoGoSearchQuery query with mnoGoSearch Syntax in it.
	 * @return query with mnoGoSearch syntax replaced for lucene
	 */
	protected String replaceBooleanMnoGoSearchQuery(final String mnoGoSearchQuery) {
		String luceneQuery = mnoGoSearchQuery.replaceAll(" ?\\| ?", " OR ").replaceAll("(?<!\\\\) ?& ?", " AND ").replace('\'', '"');
		luceneQuery = luceneQuery.replaceAll(" ~([a-zA-Z0-9üöäÜÖÄß]+)", " NOT $1");
		return luceneQuery;
	}

	protected static Logger getLogger() {
		return LOGGER;
	}

	protected Collection<String> getAttributesToSearchIn() {
		return attributesToSearchIn;
	}

	protected static int getOne() {
		return ONE;
	}
}