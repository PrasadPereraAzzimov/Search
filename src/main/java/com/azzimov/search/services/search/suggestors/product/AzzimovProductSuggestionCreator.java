package com.azzimov.search.services.search.suggestors.product;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Attribute;
import com.azzimov.search.common.dto.externals.Category;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.suggestors.AzzimovPhraseSuggestor;
import com.azzimov.search.common.suggestors.AzzimovSuggestor;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import com.azzimov.search.services.search.suggestors.AzzimovSuggestionCreator;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import java.util.ArrayList;
import java.util.List;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.SUGGEST_FIELD;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 4/18/18.
 *
 */
public class AzzimovProductSuggestionCreator extends AzzimovSuggestionCreator<AzzimovSearchParameters,
        AzzimovSuggestor> {
    private static final String SUGGEST_TEMPLATE = "{{suggestion}}";
    private static final int NUMVER_OF_SUGGESTIONS = 5;

    @Override
    public List<AzzimovSuggestor> createAzzimovSuggestion(AzzimovSearchParameters azzimovSearchParameters) {
        List<AzzimovSuggestor> azzimovPhraseSuggestorList = new ArrayList<>();
        String query = azzimovSearchParameters.getAzzimovSearchRequest().getAzzimovSearchRequestParameters().getQuery();
        // Retrieve the language field for the query language
        String[] suggestGenerateFields = new String[] {Product.PRODUCT_TITLE,
                Product.PRODUCT_SHORT_DESCRIPTION,
                Product.PRODUCT_LONG_DESCRIPTION,
                retrieveFieldPath(Product.PRODUCT_CATEGORIES, Category.CATEGORY_LABEL),
                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES, Attribute.ATTRIBUTE_LABEL),
                retrieveFieldPath(Product.PRODUCT_ATTRIBUTES, Attribute.ATTRIBUTE_STRING_VALUE)};
        LanguageCode languageCode = azzimovSearchParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        for (String field : suggestGenerateFields) {
            AzzimovPhraseSuggestor azzimovPhraseSuggestor = new AzzimovPhraseSuggestor();
            azzimovPhraseSuggestor.setField(retrieveFieldPath(field, LanguageCode.getLanguageField(languageCode),
                    SUGGEST_FIELD));
            azzimovPhraseSuggestor.setSize(NUMVER_OF_SUGGESTIONS);
            azzimovPhraseSuggestor.setSuggestionId(field);
            azzimovPhraseSuggestor.setName(field);
            azzimovPhraseSuggestor.setText(query);
            AzzimovPhraseSuggestor.SuggestCollateQuery suggestCollateQuery = new AzzimovPhraseSuggestor.SuggestCollateQuery();
            suggestCollateQuery.setQueryType(MatchPhraseQueryBuilder.NAME);
            suggestCollateQuery.setMatchTemplateField(retrieveFieldPath(field, LanguageCode.getLanguageField(languageCode),
                    SUGGEST_FIELD));
            suggestCollateQuery.setMatchTemplateValue(SUGGEST_TEMPLATE);
            suggestCollateQuery.setPrune(true);
            azzimovPhraseSuggestor.setSuggestCollateQuery(suggestCollateQuery);
            azzimovPhraseSuggestor.setSuggestMode(AzzimovSuggestor.SuggestMode.PUPULAR);
            azzimovPhraseSuggestorList.add(azzimovPhraseSuggestor);
        }
        return azzimovPhraseSuggestorList;
    }
}
