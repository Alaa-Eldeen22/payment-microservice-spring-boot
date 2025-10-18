package com.paymenthub.payment_service.application.port.in;

import com.paymenthub.payment_service.application.dto.request.AuthorizePaymentRequest;
import com.paymenthub.payment_service.application.dto.response.AuthorizationResponse;

public interface AuthorizePaymentUseCase {
    AuthorizationResponse execute(AuthorizePaymentRequest request);
}