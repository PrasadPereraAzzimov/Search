package com.azzimov.search.services.search.queries;

import com.azzimov.search.common.query.AzzimovFilterFunctionQuery;
import com.azzimov.search.common.query.AzzimovFunctionScoreQuery;
import com.azzimov.search.common.query.AzzimovMatchAllQuery;
import com.azzimov.search.common.query.AzzimovQuery;
import com.azzimov.search.common.query.AzzimovScriptQuery;
import com.azzimov.search.common.util.config.ConfigurationHandler;
import com.azzimov.search.common.util.config.SearchConfiguration;
import com.azzimov.search.services.search.params.AzzimovSearchParameters;
import javassist.scopedpool.SoftValueHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.common.query.AzzimovFilterFunctionQuery.AzzimovScoreFunctionConstant.SCRIPTSCOREFUNCTION;

/**
 * Created by prasad on 1/16/18.
 */
public class AzzimovSearchScoreAssimilatorCreator extends AzzimovQueryCreator<AzzimovSearchParameters,
        AzzimovQuery, AzzimovFunctionScoreQuery> {
    private ConfigurationHandler configurationHandler;

    public AzzimovSearchScoreAssimilatorCreator(ConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }
    @Override
    public AzzimovFunctionScoreQuery createAzzimovQuery(AzzimovSearchParameters azzimovParameters, AzzimovQuery query) {
        List<Object> configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_BOOST_VALUES);
        Map<String, Integer> searchFieldBoosts = (Map<String, Integer>) configList.get(0);
        configList = configurationHandler.getObjectConfigList(SearchConfiguration.QUERY_MINIMUM_SHOULD_MATCH);
        Map<String, Integer> minimumFieldMatch = (Map<String, Integer>) configList.get(0);
        Map<String, String> targetDocumentTypes = azzimovParameters.getTargetRepositories();
        // Here, for now, we expect one time of targets
        List<String> targetDocs = new ArrayList<>(targetDocumentTypes.keySet());
        // the target repository/index
        String targetRepository = targetDocumentTypes.values().iterator().next();
        AzzimovFunctionScoreQuery azzimovFunctionScoreQuery = new AzzimovFunctionScoreQuery(targetRepository, targetDocs);
        AzzimovFilterFunctionQuery azzimovFilterFunctionQuery = new AzzimovFilterFunctionQuery();
        List<AzzimovFilterFunctionQuery> azzimovFilterFunctionQueryList = new ArrayList<>();

        azzimovFilterFunctionQueryList.add(azzimovFilterFunctionQuery);
        azzimovFunctionScoreQuery.setFilterFunctionQueryList(azzimovFilterFunctionQueryList);
        azzimovFunctionScoreQuery.setAzzimovQuery(query);
        azzimovFilterFunctionQuery.setAzzimovScoreFunctionConstant(SCRIPTSCOREFUNCTION);
        azzimovFilterFunctionQuery.setScript(
                "Math.log10(Math.log10(_score + 1) + 1)");
        azzimovFilterFunctionQuery.setAzzimovQuery(new AzzimovMatchAllQuery(targetRepository, targetDocs));
        azzimovFunctionScoreQuery.setAzzimovScoreModeConstant(AzzimovFunctionScoreQuery.AzzimovScoreModeConstant.MULTIPLY);
        azzimovFunctionScoreQuery.setAzzimovCombineFunctionConstant(AzzimovFunctionScoreQuery.AzzimovCombineFunctionConstant.REPLACE);
        azzimovFunctionScoreQuery.setResultOffset(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultOffset());
        azzimovFunctionScoreQuery.setResultSize(azzimovParameters.getAzzimovSearchRequest()
                .getAzzimovSearchRequestParameters().getResultsPerPage());
        return azzimovFunctionScoreQuery;
    }
}
