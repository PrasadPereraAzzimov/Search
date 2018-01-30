package com.azzimov.search.services.search.sorters;

import com.azzimov.search.common.sorters.AzzimovSorter;
import com.azzimov.search.services.search.params.AzzimovParameters;

import java.util.List;

/**
 * Created by prasad on 1/25/18.
 * AzzimovSorterCreator used to implement concrete search sorters for the Azzimov Search Application
 */
public abstract class AzzimovSorterCreator <Parameters extends AzzimovParameters,
        SorterI extends AzzimovSorter, SorterO extends AzzimovSorter> {
    public abstract List<SorterO> createAzzimovQuery(Parameters azzimovParameters, List<SorterI> sorterIList);
}
