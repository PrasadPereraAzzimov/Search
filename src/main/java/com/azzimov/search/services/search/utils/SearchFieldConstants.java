package com.azzimov.search.services.search.utils;

import com.azzimov.search.common.dto.externals.ExternalDTO;

/**
 * Created by prasad on 1/30/18.
 * For now, we put all the shared field type constants and operations here
 */
public class SearchFieldConstants {
    // For now, we put all the shared field type constants and operations here
    // for now, this will be here, in future it should be within the dto libs
    public static final String EXACT_FIELD = "raw_lower";
    public static final String EXACT_FIELD_RAW = "raw";
    public static final String VALUE_NUM = "value_num";
    public static final String VALUE_DATE = "value_date";
    public static final String VALUE_TEXT = "value_text";
    public static final String SUGGEST_FIELD = "suggest";

    public static String retrieveFieldPath(String... path) {
        String fieldPath = "";
        for (String p : path) {
            fieldPath += (p + ExternalDTO.EXTERNAL_FIELD_DELIMITER);
        }
        return fieldPath.substring(0, fieldPath.length() - 1);
    }
}
