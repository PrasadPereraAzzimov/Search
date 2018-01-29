package com.azzimov.search.services.search.filters;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.AzzimovRequestRefinement;
import com.azzimov.search.common.dto.externals.Category;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.query.AzzimovBooleanQuery;
import com.azzimov.search.common.query.AzzimovTermTermQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.azzimov.search.services.search.queries.AzzimovProductSearchExactQueryCreator.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.queries.AzzimovQueryCreator.retrieveFieldPath;

/**
 * Created by prasad on 1/25/18.
 * AzzimovProductSearchRefinementFilterCreator provides the product related search refinements (category refinements)
 */
public class AzzimovProductSearchRefinementFilterCreator  extends AzzimovFilterCreator<AzzimovSearchParameters,
        AzzimovBooleanQuery, AzzimovBooleanQuery> {
    private ConfigurationHandler configurationHandler;

    public AzzimovProductSearchRefinementFilterCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public AzzimovBooleanQuery createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                           AzzimovBooleanQuery azzimovQueries) {
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        String query = azzimovParameters.getAzzimovSearchRequest().
                getAzzimovSearchRequestParameters().getQuery();
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>(targetDocumentTypes.keySet());
        // the target repository/index
        String targetRepository = targetDocumentTypes.values().iterator().next();
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();

        AzzimovRequestRefinement azzimovRequestRefinement = azzimovParameters.
                getAzzimovSearchRequest().getAzzimovSearchRequestParameters().getAzzimovRequestRefinement();
        if (azzimovRequestRefinement != null) {
            AzzimovBooleanQuery filterBooleanQuery = new AzzimovBooleanQuery(targetRepository, targetDocs);
            while (azzimovRequestRefinement != null) {
                AzzimovTermTermQuery azzimovTermTermQuery = new AzzimovTermTermQuery(targetRepository,
                        retrieveFieldPath(Product.PRODUCT_CATEGORIES,
                                Category.CATEGORY_LABEL,
                                LanguageCode.getLanguageField(languageCode),
                                EXACT_FIELD_RAW), targetDocs, azzimovRequestRefinement.getValue());
                filterBooleanQuery.addMustQuery(azzimovTermTermQuery);
                azzimovRequestRefinement = azzimovRequestRefinement.getChildAzzimovRequestRefinement();
            }
            azzimovQueries.addFilterQuery(filterBooleanQuery);
        }
        return azzimovQueries;
    }
}
