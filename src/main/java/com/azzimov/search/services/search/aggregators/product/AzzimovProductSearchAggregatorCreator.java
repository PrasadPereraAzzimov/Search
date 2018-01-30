package com.azzimov.search.services.search.aggregators.product;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.aggregators.AzzimovTermAggregator;
import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.externals.ProductGuidance;
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
    public List<AzzimovAggregator> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                      List<AzzimovAggregator> azzimovAggregator) {
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

        azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE);
        azzimovTermAggregator.setField(
                retrieveFieldPath(Product.PRODUCT_GUIDANCE,
                        ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE,
                        LanguageCode.getLanguageField(languageCode)));
        azzimovTermAggregator.setSize(searchAggregationLimits.get(SearchConfiguration.SEARCH_CATEGORY_AGGR_LIMIT));
        azzimovTermAggregatorList.add(azzimovTermAggregator);

        azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_OTHER_GUIDANCE);
        azzimovTermAggregator.setField(
                retrieveFieldPath(Product.PRODUCT_GUIDANCE,
                        ProductGuidance.PRODUCT_GUIDANCE_OTHER_GUIDANCE,
                        LanguageCode.getLanguageField(languageCode)));
        azzimovTermAggregator.setSize(searchAggregationLimits.get(SearchConfiguration.SEARCH_CATEGORY_AGGR_LIMIT));
        azzimovTermAggregatorList.add(azzimovTermAggregator);
        return azzimovTermAggregatorList;
    }
}
