package com.azzimov.search.services.search.suggestors;

import com.azzimov.search.common.suggestors.AzzimovSuggestor;
import com.azzimov.search.services.search.params.AzzimovParameters;

import java.util.List;

/**
 * Created by prasad on 4/18/18.
 *
 */
public abstract class AzzimovSuggestionCreator <Parameters extends AzzimovParameters, SuggestO extends AzzimovSuggestor> {
    public abstract List<SuggestO> createAzzimovSuggestion(Parameters azzimovParameters);
}
