package com.azzimov.search.services.search.aggregators;

import com.azzimov.search.common.aggregators.AzzimovAggregator;
import com.azzimov.search.common.aggregators.AzzimovTermAggregator;
import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.externals.Category;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.dto.externals.ProductGuidance;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;

import java.util.ArrayList;
import java.util.List;

import static com.azzimov.search.services.search.queries.AzzimovQueryCreator.retrieveFieldPath;

/**
 * Created by prasad on 1/16/18.
 */
public class AzzimovProductSearchAggregatorCreator extends AzzimovAggregatorCreator<AzzimovSearchParameters,
        AzzimovAggregator, AzzimovAggregator> {
    @Override
    public List<AzzimovAggregator> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                                   List<AzzimovAggregator> azzimovAggregator) {
        // we dont use the input aggregator in these cases
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();
        AzzimovTermAggregator azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_ATTRIBUTE_GUIDANCE);
        azzimovTermAggregator.setField("guidances.attributes.en");
        azzimovTermAggregator.setSize(100);
        List<AzzimovAggregator> azzimovTermAggregatorList = new ArrayList<>();
        azzimovTermAggregatorList.add(azzimovTermAggregator);

        azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE);
        azzimovTermAggregator.setField("guidances.level1.en");
        azzimovTermAggregator.setSize(100);
        azzimovTermAggregatorList.add(azzimovTermAggregator);

        azzimovTermAggregator = new AzzimovTermAggregator();
        azzimovTermAggregator.setName(ProductGuidance.PRODUCT_GUIDANCE_OTHER_GUIDANCE);
        azzimovTermAggregator.setField("guidances.others.en");
        azzimovTermAggregator.setSize(100);
        azzimovTermAggregatorList.add(azzimovTermAggregator);
        return azzimovTermAggregatorList;
    }
}
