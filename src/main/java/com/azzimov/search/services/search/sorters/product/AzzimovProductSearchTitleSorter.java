package com.azzimov.search.services.search.sorters.product;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchSortRequestParameters;
import com.azzimov.search.common.dto.externals.Product;
import com.azzimov.search.common.sorters.AzzimovFieldSorter;
import com.azzimov.search.common.sorters.AzzimovSorter;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.services.search.params.product.AzzimovSearchParameters;
import java.util.List;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.utils.SearchFieldConstants.retrieveFieldPath;

/**
 * Created by prasad on 2/1/18.
 */
public class AzzimovProductSearchTitleSorter extends AzzimovProductSearchSorterCreator {
    public AzzimovProductSearchTitleSorter(ConfigurationHandler configurationHandler) {
        super(configurationHandler, null);
    }

    @Override
    public List<AzzimovSorter> createAzzimovSorter(AzzimovSearchParameters azzimovParameters,
                                                   List<AzzimovSorter> azzimovSorterList) {
        // In this case, we will build an azzimov query based on our search parameter query terms
        // The field specific query we build here is a concrete implementation of our product search keyword query for
        // product documents
        // For the Azzimov Product Search query creator, we will not consider the input query azzimov query as this
        // is the base/core of product query
        // Retrieve the language field for the query language
        LanguageCode languageCode = azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getLanguage().getLanguageCode();
        AzzimovSearchSortRequestParameters sortRequestParameters =
                azzimovParameters.getAzzimovSearchRequest().
                        getAzzimovSearchRequestParameters().
                        getAzzimovSearchSortRequestParameters();
        if (sortRequestParameters != null) {
            String sortField = retrieveFieldPath(Product.PRODUCT_TITLE, EXACT_FIELD_RAW,
                    LanguageCode.getLanguageField(languageCode));
            AzzimovSorter azzimovSorter = new AzzimovFieldSorter(sortField);
            azzimovSorter.setSortOrder(sortRequestParameters.getAzzimovSortOrder());
            azzimovSorter.setAzzimovSorterMode(sortRequestParameters.getAzzimovSorterMode());
            azzimovSorterList.add(azzimovSorter);
        }
        return azzimovSorterList;
    }
}
