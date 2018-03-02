package com.azzimov.search.services.search.queries.product;

import com.azzimov.search.common.dto.Language;
import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Category;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.query.AzzimovMatchPhraseQuery;
import com.azzimov.search.common.query.AzzimovNestedQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.query.AzzimovQueryStringQuery;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.text.AzzimovTextQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.queries.AzzimovQueryCreator;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 1/11/18.
 * This a template implmentation of AzzimovRequestCreator. The idea is to iteratively build Azzimov Search Requests
 * depending on the requirements.
 */
public class AzzimovProductSearchQueryCreator extends AzzimovQueryCreator<AzzimovSearchParameters,
        AzzimovQuery, AzzimovBooleanQuery> {
    private ConfigurationHandler configurationHandler;
    private static Map<Language, List<String>> mapStopwordListMap = createStopwordListMap();

    public AzzimovProductSearchQueryCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public AzzimovBooleanQuery createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                  AzzimovQuery azzimovQuery) {
        List<Object>  configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_BOOST_VALUES);
        Map<String, Integer> searchFieldBoosts = (Map<String, Integer>) configList.get(0);
        configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_MINIMUM_SHOULD_MATCH);
        Map<String, Integer> minimumFieldMatch = (Map<String, Integer>) configList.get(0);
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        String query = azzimovParameters.getAzzimovSearchRequest().
                getAzzimovSearchRequestParameters().getQuery();
        // Retrieve the language field for the query language
        Locale locale = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLocale();
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        // Retrieve normalized query
        AzzimovTextQuery azzimovTextQuery;
        String normalizedQuery = query;
        AzzimovTextProcessor azzimovTextProcessor = new AzzimovTextProcessor();
        try {
            List<String> stopwordList = mapStopwordListMap.get(
                    azzimovParameters.getAzzimovSearchRequest().getAzzimovSearchRequestParameters().getLanguage());
            azzimovTextQuery  = azzimovTextProcessor.retrieveNormalizedQuery(query, locale, stopwordList);
            normalizedQuery = azzimovTextQuery.getProcessedQueryString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>();
        targetDocs.add(Product.PRODUCT_EXTERNAL_NAME);
        // the target repository/index
        String targetRepository = targetDocumentTypes.get(Product.PRODUCT_EXTERNAL_NAME);
        // pre process the query as we want here
        AzzimovBooleanQuery azzimovBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);

        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryTitle = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_TITLE, LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_TITLE));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(minimumFieldMatch.get(SearchConfiguration.SEARCH_TITLE));
        azzimovBooleanQuery.addShouldQuery(azzimovMatchPhraseQueryTitle);
        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryCategory = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Category.CATEGORY_EXTERNAL_NAME,
                        Category.CATEGORY_LABEL,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_CATEGORY_LABEL));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_CATEGORY_LABEL));
        azzimovBooleanQuery.addShouldQuery(azzimovMatchPhraseQueryCategory);

        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryAttributeValueStr = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                        Attribute.ATTRIBUTE_STRING_VALUE,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
        AzzimovNestedQuery azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
        azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
        azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
        azzimovNestedQuery.setAzzimovQuery(azzimovMatchPhraseQueryAttributeValueStr);
        azzimovBooleanQuery.addShouldQuery(azzimovNestedQuery);

        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryAttributeValueNum = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                        Attribute.ATTRIBUTE_NUMBER_VALUE,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_ATTRIBUTE_VALUE));
        azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
        azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
        azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
        azzimovNestedQuery.setAzzimovQuery(azzimovMatchPhraseQueryAttributeValueNum);
        azzimovBooleanQuery.addShouldQuery(azzimovNestedQuery);


        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryAttributeLabel = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES,
                        Attribute.ATTRIBUTE_LABEL,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_ATTRIBUTE_LABEL));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_ATTRIBUTE_LABEL));
        azzimovNestedQuery = new AzzimovNestedQuery(targetRepository, targetDocs);
        azzimovNestedQuery.setPath(Product.PRODUCT_ATTRIBUTES);
        azzimovNestedQuery.setScoreMode(AzzimovNestedQuery.AzzimovNestedScoreMode.MAX);
        azzimovNestedQuery.setAzzimovQuery(azzimovMatchPhraseQueryAttributeLabel);
        azzimovBooleanQuery.addShouldQuery(azzimovNestedQuery);


        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryShortDesc = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_SHORT_DESCRIPTION,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_SHORT_DESCRIPTION));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_SHORT_DESCRIPTION));
        azzimovBooleanQuery.addShouldQuery(azzimovMatchPhraseQueryShortDesc);

        AzzimovMatchPhraseQuery azzimovMatchPhraseQueryLongDesc = new AzzimovMatchPhraseQuery(targetRepository,
                retrieveFieldPath(Product.PRODUCT_LONG_DESCRIPTION,
                        LanguageCode.getLanguageField(languageCode)),
                targetDocs,
                normalizedQuery);
        azzimovMatchPhraseQueryTitle.setQueryBoost(searchFieldBoosts.get(SearchConfiguration.SEARCH_LONG_DESCRIPTION));
        azzimovMatchPhraseQueryTitle.setQueryMinimumShouldMatch(
                minimumFieldMatch.get(SearchConfiguration.SEARCH_LONG_DESCRIPTION));
        azzimovBooleanQuery.addShouldQuery(azzimovMatchPhraseQueryLongDesc);

        List<String> allQueryTargetFields = new ArrayList<>();
        // For empty string, we create a match all query
        if (normalizedQuery.isEmpty()) {
            AzzimovMatchAllQuery azzimovMatchAllQuery = new AzzimovMatchAllQuery(targetRepository,
                    targetDocs);
            azzimovBooleanQuery.addMustQuery(azzimovMatchAllQuery);
        } else {
            AzzimovQueryStringQuery azzimovQueryStringQuery = new AzzimovQueryStringQuery(targetRepository,
                    allQueryTargetFields,
                    targetDocs,
                    normalizedQuery);
            azzimovQueryStringQuery.setQueryMinimumShouldMatch(
                    minimumFieldMatch.get(SearchConfiguration.SEARCH_ALL));
            azzimovBooleanQuery.addMustQuery(azzimovQueryStringQuery);
        }
        azzimovBooleanQuery.setResultOffset(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultOffset());
        azzimovBooleanQuery.setResultSize(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultsPerPage());
        return azzimovBooleanQuery;
    }

    private static Map<Language, List<String>> createStopwordListMap() {
        Language en = new Language(LanguageCode.EN_US);
        Iterator<Object> enStopwordsIterator = EnglishAnalyzer.getDefaultStopSet().iterator();
        Map<Language, List<String>> stopwordMap = new TreeMap<>(
                (l1, l2) -> l1.getLanguageCode().getValue().compareTo(l2.getLanguageCode().getValue()));
        List<String> enStopwords = new ArrayList<>();
        while (enStopwordsIterator.hasNext()) {
            char[] stopWord = (char[]) enStopwordsIterator.next();
            enStopwords.add(String.valueOf(stopWord));
        }
        stopwordMap.put(en, enStopwords);

        Language fr = new Language(LanguageCode.FR_CA);
        enStopwordsIterator = FrenchAnalyzer.getDefaultStopSet().iterator();
        enStopwords = new ArrayList<>();
        while (enStopwordsIterator.hasNext()) {
            char[] stopWord = (char[]) enStopwordsIterator.next();
            enStopwords.add(String.valueOf(stopWord));
        }
        stopwordMap.put(fr, enStopwords);
        return stopwordMap;
    }
}
