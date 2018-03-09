package com.azzimov.search.services.feedback;

import com.azzimov.search.common.dto.communications.requests.search.AzzimovSearchRequest;
import com.azzimov.search.common.dto.communications.responses.search.AzzimovSearchResponse;
import com.azzimov.search.common.dto.internals.feedback.Feedback;
import java.util.List;

/**
 * Created by prasad on 1/19/18.
 * FeedbackPersistRequest is used when forwarding already build product type feedback or providing data (search related)
 * to built feedbacks (for query/guidance etc.)
 */
public class AzzimovFeedbackPersistRequest {
    private AzzimovSearchRequest azzimovSearchRequest;
    private AzzimovSearchResponse azzimovSearchResponse;
    private List<? extends Feedback> feedbackList;


    public AzzimovSearchRequest getAzzimovSearchRequest() {
        return azzimovSearchRequest;
    }

    public void setAzzimovSearchRequest(AzzimovSearchRequest azzimovSearchRequest) {
        this.azzimovSearchRequest = azzimovSearchRequest;
    }

    public List<? extends Feedback> getFeedbackList() {
        return feedbackList;
    }

    public void setFeedbackList(List<? extends Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    public AzzimovSearchResponse getAzzimovSearchResponse() {
        return azzimovSearchResponse;
    }

    public void setAzzimovSearchResponse(AzzimovSearchResponse azzimovSearchResponse) {
        this.azzimovSearchResponse = azzimovSearchResponse;
    }
}
