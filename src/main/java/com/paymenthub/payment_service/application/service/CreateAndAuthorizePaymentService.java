package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateAndAuthorizePaymentService implements CreateAndAuthorizePaymentUseCase {

    private final CreatePendingPaymentService createPaymentService;
    private final AuthorizePaymentService authorizePaymentService;

    @Override
    @Transactional
    public PaymentResult createAndAuthorize(CreatePaymentCommand command) {

        Payment pendingPayment = createPaymentService.create(new CreatePaymentCommand(
                command.invoiceId(),
                command.customerId(),
                command.amount(),
                command.currency(),
                command.paymentMethodId()));

        Payment authorizedPayment = authorizePaymentService.authorize(pendingPayment);

        return PaymentResult.fromDomain(authorizedPayment);
    }
}