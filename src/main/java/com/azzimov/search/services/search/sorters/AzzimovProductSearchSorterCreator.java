package com.azzimov.search.services.search.sorters;

import com.azzimov.search.common.dto.LanguageCode;
import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchSortRequestParameters;
import com.azzimov.search.common.sorters.AzzimovFieldSorter;
import com.azzimov.search.common.sorters.AzzimovSorter;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;
import java.util.List;
import java.util.Map;
import static com.azzimov.search.services.search.queries.AzzimovProductSearchExactQueryCreator.EXACT_FIELD_RAW;
import static com.azzimov.search.services.search.queries.AzzimovQueryCreator.retrieveFieldPath;

/**
 * Created by prasad on 1/25/18.
 * AzzimovProductSearchSorterCreator provides the Azzimov Product related search sorters
 */
public class AzzimovProductSearchSorterCreator extends AzzimovSorterCreator<AzzimovSearchParameters,
        AzzimovSorter, AzzimovSorter> {
    private ConfigurationHandler configurationHandler;
    public AzzimovProductSearchSorterCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public List<AzzimovSorter> createAzzimovQuery(AzzimovSearchParameters azzimovParameters,
                                           List<AzzimovSorter> azzimovSorterList) {
        List<Object> configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_SORT_FIELDS);
        Map<String, String> querySortFields = (Map<String, String>) configList.get(0);
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
            String sortField = retrieveFieldPath(querySortFields.get(sortRequestParameters.getField()), EXACT_FIELD_RAW,
                    LanguageCode.getLanguageField(languageCode));
            AzzimovSorter azzimovSorter = new AzzimovFieldSorter(sortField);
            azzimovSorter.setSortOrder(sortRequestParameters.getAzzimovSortOrder());
            azzimovSorter.setAzzimovSorterMode(sortRequestParameters.getAzzimovSorterMode());
            azzimovSorterList.add(azzimovSorter);
        }
        return azzimovSorterList;
    }
}
