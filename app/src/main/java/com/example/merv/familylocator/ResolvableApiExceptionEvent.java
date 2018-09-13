package com.example.merv.familylocator;

import com.google.android.gms.common.api.ResolvableApiException;

public class ResolvableApiExceptionEvent {
    private ResolvableApiException exception;
    public ResolvableApiExceptionEvent(ResolvableApiException exception){
        this.exception = exception;
    }
    public ResolvableApiException getException () {
        return exception;
    }
}
