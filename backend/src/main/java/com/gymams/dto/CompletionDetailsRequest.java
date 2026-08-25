package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;

/** Technician's completion notes — mandatory when marking a repair or maintenance task COMPLETED. */
public class CompletionDetailsRequest {

    @NotBlank(message = "Enter completion details.")
    private String completionDetails;

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }
}
