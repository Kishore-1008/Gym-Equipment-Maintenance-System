package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin's rejection of a PENDING repair request — reason is mandatory. */
public class RepairRequestRejectRequest {

    @NotBlank(message = "A rejection reason is required.")
    private String rejectionReason;

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
