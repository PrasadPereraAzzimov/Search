package com.azzimov.search.services.search.reponses;

import com.azzimov.search.common.dto.SearchType;
import com.azzimov.search.common.dto.communications.responses.AzzimovResponseStatus;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchInfo;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponseParameter;
import com.azzimov.search.common.dto.externals.Guidance;
import com.azzimov.search.common.dto.externals.GuidanceCategory;
import com.azzimov.search.common.dto.externals.GuidanceFilter;
import com.azzimov.search.common.dto.externals.GuidanceFilterValue;
import com.azzimov.search.common.dto.externals.GuidanceSubCategory;
import com.azzimov.search.common.dto.externals.ProductGuidance;
import com.azzimov.search.common.requests.AzzimovSearchRequest;
import com.azzimov.search.common.responses.AzzimovAggregationResponse;
import com.azzimov.search.common.responses.AzzimovSearchHitResponse;
import com.azzimov.search.common.responses.AzzimovSearchResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.azzimov.search.services.search.utils.SearchFieldConstants.VALUE_TEXT;

/**
 * Created by prasad on 1/17/18.
 * AzzimovSearchResponseBuilder builds a search response to be replied with
 */
public class AzzimovSearchResponseBuilder {
    private AzzimovSearchResponse azzimovSearchResponse;
    private AzzimovSearchRequest azzimovSearchRequest;
    public static final String PRODUCT_PREFIX = "prod::";

    public AzzimovSearchResponseBuilder(AzzimovSearchResponse azzimovSearchResponse,
                                        AzzimovSearchRequest azzimovSearchRequest) {
        this.azzimovSearchResponse = azzimovSearchResponse;
        this.azzimovSearchRequest = azzimovSearchRequest;
    }

    public com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse build() {
        com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse azzimovSearchResponseOut =
                new com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse();
        AzzimovResponseStatus azzimovResponseStatus = new
                AzzimovResponseStatus(AzzimovResponseStatus.ResponseInformation.STATUS_OK);
        azzimovSearchResponseOut.setAzzimovResponseStatus(azzimovResponseStatus);
        AzzimovSearchInfo azzimovSearchInfo = new AzzimovSearchInfo();
        azzimovSearchInfo.setCount(azzimovSearchResponse.getTotalHits());
        azzimovSearchInfo.setSearchType(SearchType.EXACT);
        azzimovSearchResponseOut.setAzzimovSearchInfo(azzimovSearchInfo);

        AzzimovSearchResponseParameter azzimovSearchResponseParameter = new AzzimovSearchResponseParameter();
        List<Long> productIds = new ArrayList<>();
        for (AzzimovSearchHitResponse searchHitResponse : azzimovSearchResponse.getAzzimovSearchHitResponseList()){
            String productId = searchHitResponse.getId().substring(
                    searchHitResponse.getId().indexOf(PRODUCT_PREFIX) + PRODUCT_PREFIX.length());
            productIds.add(Long.parseLong(productId));
        }
        azzimovSearchResponseParameter.setProductIds(productIds);

        Guidance guidance = new Guidance();
        List<GuidanceCategory> guidanceCategoryList = new ArrayList<>();
        List<GuidanceFilter> guidanceFilterList = new ArrayList<>();

        if (azzimovSearchResponse.getAzzimovAggregationResponseList() != null &&
                !azzimovSearchResponse.getAzzimovAggregationResponseList().isEmpty()) {
            for (AzzimovAggregationResponse azzimovAggregationResponse :
                    azzimovSearchResponse.getAzzimovAggregationResponseList()) {
                if (azzimovAggregationResponse.getName().equals(ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE)) {
                    guidanceCategoryList.addAll(createCategoryResults(azzimovAggregationResponse));
                }
                if (azzimovAggregationResponse.getName().equals(ProductGuidance.PRODUCT_GUIDANCE_PARENT_GUIDANCE)) {
                    guidanceCategoryList.addAll(createSubCategoryResults(azzimovAggregationResponse));
                }
                if (azzimovAggregationResponse.getName().equals(ProductGuidance.PRODUCT_GUIDANCE_ATTRIBUTE_GUIDANCE)) {
                    guidanceFilterList.addAll(createAttributeResults(azzimovAggregationResponse));
                }
            }
        }
        guidance.setGuidanceCategories(guidanceCategoryList);
        guidance.setGuidanceFilters(guidanceFilterList);
        azzimovSearchResponseParameter.setGuidance(guidance);
        azzimovSearchResponseOut.setAzzimovSearchResponseParameter(azzimovSearchResponseParameter);
        return azzimovSearchResponseOut;
    }


    private static Collection<GuidanceCategory> createCategoryResults(AzzimovAggregationResponse azzimovAggregationResponse) {
        Map<String, GuidanceCategory> guidanceCategoryMap = new HashMap<>();
        for (Map.Entry<String, Long> aggEntry : azzimovAggregationResponse.getFieldCountMap().entrySet()) {
            String[] categoryLabelValuePair = aggEntry.getKey().split(ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR);
            if (guidanceCategoryMap.containsKey(categoryLabelValuePair[0].trim())) {
                GuidanceSubCategory guidanceSubCategory = new GuidanceSubCategory();
                guidanceSubCategory.setCategoryName(categoryLabelValuePair[1].trim());
                guidanceSubCategory.setCount(aggEntry.getValue());
                guidanceCategoryMap.get(categoryLabelValuePair[0].trim())
                        .getGuidanceSubCategories().add(guidanceSubCategory);
                guidanceCategoryMap.get(categoryLabelValuePair[0].trim())
                        .setSubCategoryCount(guidanceCategoryMap.get(
                                categoryLabelValuePair[0].trim()).getSubCategoryCount() + 1);
            } else {
                GuidanceSubCategory guidanceSubCategory = new GuidanceSubCategory();
                guidanceSubCategory.setCategoryName(categoryLabelValuePair[1].trim());
                guidanceSubCategory.setCount(aggEntry.getValue());
                GuidanceCategory guidanceCategory = new GuidanceCategory();
                List<GuidanceSubCategory> guidanceSubCategoryList = new ArrayList<>();
                guidanceSubCategoryList.add(guidanceSubCategory);
                guidanceCategory.setGuidanceSubCategories(guidanceSubCategoryList);
                guidanceCategoryMap.put(categoryLabelValuePair[0].trim(), guidanceCategory);
                guidanceCategory.setCategoryName(ProductGuidance.PRODUCT_GUIDANCE_LEVEL1_GUIDANCE);
                guidanceCategory.setSubCategoryCount(1);
            }
        }
        return guidanceCategoryMap.values();
    }

    private static Collection<GuidanceCategory> createSubCategoryResults(AzzimovAggregationResponse azzimovAggregationResponse) {
        Map<String, GuidanceCategory> guidanceCategoryMap = new HashMap<>();
        for (Map.Entry<String, Long> aggEntry : azzimovAggregationResponse.getFieldCountMap().entrySet()) {
            String[] categoryLabelValuePair = aggEntry.getKey().split(ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR);
            if (guidanceCategoryMap.containsKey(categoryLabelValuePair[1].trim())) {
                GuidanceSubCategory guidanceSubCategory = new GuidanceSubCategory();
                guidanceSubCategory.setCategoryName(categoryLabelValuePair[0].trim());
                guidanceSubCategory.setCount(aggEntry.getValue());
                guidanceCategoryMap.get(categoryLabelValuePair[1].trim())
                        .getGuidanceSubCategories().add(guidanceSubCategory);
                guidanceCategoryMap.get(categoryLabelValuePair[1].trim())
                        .setSubCategoryCount(guidanceCategoryMap.get(
                                categoryLabelValuePair[1].trim()).getSubCategoryCount() + 1);
            } else {
                GuidanceSubCategory guidanceSubCategory = new GuidanceSubCategory();
                guidanceSubCategory.setCategoryName(categoryLabelValuePair[0].trim());
                guidanceSubCategory.setCount(aggEntry.getValue());
                GuidanceCategory guidanceCategory = new GuidanceCategory();
                List<GuidanceSubCategory> guidanceSubCategoryList = new ArrayList<>();
                guidanceSubCategoryList.add(guidanceSubCategory);
                guidanceCategory.setGuidanceSubCategories(guidanceSubCategoryList);
                guidanceCategoryMap.put(categoryLabelValuePair[1].trim(), guidanceCategory);
                guidanceCategory.setCategoryName(ProductGuidance.PRODUCT_GUIDANCE_OTHER_GUIDANCE);
                guidanceCategory.setSubCategoryCount(1);
            }
        }
        return guidanceCategoryMap.values();
    }

    private static Collection<GuidanceFilter> createAttributeResults(AzzimovAggregationResponse azzimovAggregationResponse) {
        Map<String, GuidanceFilter> guidanceFiltersMap = new HashMap<>();
        for (Map.Entry<String, Long> aggEntry : azzimovAggregationResponse.getFieldCountMap().entrySet()) {
            String[] attributeLabelValuePair = aggEntry.getKey().split(ProductGuidance.PRODUCT_GUIDANCE_SEPARATOR);
            if (guidanceFiltersMap.containsKey(attributeLabelValuePair[0].trim())) {
                GuidanceFilterValue guidanceFilterValue = new GuidanceFilterValue();
                guidanceFilterValue.setValue(attributeLabelValuePair[1].trim());
                guidanceFilterValue.setValueCount(aggEntry.getValue());
                guidanceFiltersMap.get(attributeLabelValuePair[0].trim())
                        .getGuidanceFilterValues().add(guidanceFilterValue);
                guidanceFiltersMap.get(attributeLabelValuePair[0].trim())
                        .setCount(guidanceFiltersMap.get(attributeLabelValuePair[0].trim()).getCount() + 1);
            } else {
                GuidanceFilterValue guidanceFilterValue = new GuidanceFilterValue();
                guidanceFilterValue.setValue(attributeLabelValuePair[1].trim());
                List<GuidanceFilterValue> guidanceFilterValueList = new ArrayList<>();
                guidanceFilterValueList.add(guidanceFilterValue);
                GuidanceFilter guidanceFilter = new GuidanceFilter();
                guidanceFilter.setLabel(attributeLabelValuePair[0].trim());
                guidanceFilterValue.setValueCount(aggEntry.getValue());
                guidanceFilter.setCount(1);
                guidanceFilter.setGuidanceFilterValues(guidanceFilterValueList);
                guidanceFiltersMap.put(attributeLabelValuePair[0].trim(), guidanceFilter);
                if (attributeLabelValuePair.length > 2) {
                    // This means this a numeric field
                    guidanceFilter.setFilterType(attributeLabelValuePair[2]);
                } else {
                    guidanceFilter.setFilterType(VALUE_TEXT);
                }
            }
        }
        return guidanceFiltersMap.values();
    }
}
