package com.azzimov.search.services.search.aggregators.product;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.aggregators.AzzimovTermAggregator;
import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.AzzimovRequestRefinement;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.externals.ProductGuidance;
import com.azzimov.search.common.query.AzzimovRegexpFlag;
import com.azzimov.search.common.text.AzzimovTextProcessor;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.aggregators.AzzimovAggregatorCreator;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 1/16/18.
 * AzzimovProductSearchAggregatorCreator provides impl for AzzimovAggregatorCreator that creates product related
 * search aggregations
 */
public class AzzimovProductSearchAggregatorCreator extends AzzimovAggregatorCreator<AzzimovSearchParameters,
        AzzimovAggregator, AzzimovAggregator> {
    private ConfigurationHandler configurationHandler;

    public AzzimovProductSearchAggregatorCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public List<AzzimovAggregator> createAzzimovQuery(AzzimovSearchParameters azzimovParameters, AzzimovAggregator azzimovAggregator) {
        // we dont use the input aggregator in these cases
        // Retrieve the language field for the query language
        List<Object> configList = configurationHandler.getObjectConfigList(SearchConfiguration.SEARCH_AGGR_LIMIT);
        Map<String, Integer> searchAggregationLimits = (Map<String, Integer>) configList.get(0);

        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();
        AzzimovTermAggregator azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_ATTRIBUTE_GUIDANCE);
        azzimovTermAggregator.setField(
                retrieveFieldPath(Product.PRODUCT_GUIDANCE,
                        ProductGuidance.PRODUCT_GUIDANCE_ATTRIBUTE_GUIDANCE,
                        LanguageCode.getLanguageField(languageCode)));
        azzimovTermAggregator.setSize(searchAggregationLimits.get(SearchConfiguration.SEARCH_ATTRIBUTE_AGGR_LIMIT));
        List<AzzimovAggregator> azzimovTermAggregatorList = new ArrayList<>();
        azzimovTermAggregatorList.add(azzimovTermAggregator);

        // Take the last refinement level and retrieve parent related aggregations
        AzzimovRequestRefinement azzimovRequestRefinement = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getAzzimovRequestRefinement();
        AzzimovRequestRefinement azzimovRequestRefinementParent = null;
        while (azzimovRequestRefinement != null) {
            azzimovRequestRefinementParent = azzimovRequestRefinement;
            azzimovRequestRefinement = azzimovRequestRefinement.getChildAzzimovRequestRefinement();
        }

        if(azzimovRequestRefinementParent != null) {
            azzimovTermAggregator = new AzzimovTermAggregator();
            azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_PARENT_GUIDANCE);
            azzimovTermAggregator.setField(
                    retrieveFieldPath(Product.PRODUCT_GUIDANCE,
                            ProductGuidance.PRODUCT_GUIDANCE_PARENT_GUIDANCE,
                            LanguageCode.getLanguageField(languageCode)));
            azzimovTermAggregator.setSize(searchAggregationLimits.get(SearchConfiguration.SEARCH_CATEGORY_AGGR_LIMIT));
            azzimovTermAggregator.setAzzimovTermIncludeExclude(
                    new AzzimovTermAggregator.AzzimovTermIncludeExclude(".*" + ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR
                                    + AzzimovTextProcessor.
                                    retrieveNonAlphaNumericEscapedText(azzimovRequestRefinementParent.getValue()),
                            AzzimovRegexpFlag.ALL,
                            "",
                            AzzimovRegexpFlag.NONE));
            azzimovTermAggregatorList.add(azzimovTermAggregator);

        } else {
            azzimovTermAggregator = new AzzimovTermAggregator();
            azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE);
            azzimovTermAggregator.setField(
                    retrieveFieldPath(Product.PRODUCT_GUIDANCE,
                            ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE,
                            LanguageCode.getLanguageField(languageCode)));
            azzimovTermAggregator.setSize(searchAggregationLimits.get(SearchConfiguration.SEARCH_CATEGORY_AGGR_LIMIT));
            azzimovTermAggregatorList.add(azzimovTermAggregator);
        }
        return azzimovTermAggregatorList;
    }
}
