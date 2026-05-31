package com.recred.backend.infrastructure.web;

public class SuggestionDTO {
    private String category;
    private String action;
    private String reason;

    public SuggestionDTO() {
    }

    public SuggestionDTO(String category, String action, String reason) {
        this.category = category;
        this.action = action;
        this.reason = reason;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
