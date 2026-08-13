package com.gymams.dto;

/** Generic { "message": "..." } success response — mirrors ErrorResponse's shape for symmetry. */
public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
