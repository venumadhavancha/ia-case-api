package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.CachingAppealReferenceNumberGenerator;

@Service
public class AppealReferenceNumberHandler implements PreSubmitCallbackHandler<AsylumCase> {

    private static final String DRAFT = "DRAFT";
    private final CachingAppealReferenceNumberGenerator appealReferenceNumberGenerator;

    public AppealReferenceNumberHandler(
        CachingAppealReferenceNumberGenerator appealReferenceNumberGenerator
    ) {
        this.appealReferenceNumberGenerator = appealReferenceNumberGenerator;
    }

    @Override
    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT
               && (callback.getEvent() == Event.START_APPEAL
                   || callback.getEvent() == Event.SUBMIT_APPEAL);
    }

    @Override
    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback) {

        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

        if (callback.getEvent() == Event.START_APPEAL) {
            asylumCase.setAppealReferenceNumber(DRAFT);
            return new PreSubmitCallbackResponse<>(asylumCase);
        }

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            new PreSubmitCallbackResponse<>(asylumCase);

        Optional<String> existingAppealReferenceNumber = asylumCase.getAppealReferenceNumber();

        if (!existingAppealReferenceNumber.isPresent()
            || existingAppealReferenceNumber.get().equals(DRAFT)) {

            String appealType = asylumCase.getAppealType()
                .orElseThrow(() -> new IllegalStateException("Unrecognised asylum case type"));

            Optional<String> appealReferenceNumber =
                appealReferenceNumberGenerator.getNextAppealReferenceNumberFor(
                    callback.getCaseDetails().getId(),
                    appealType
                );

            if (!appealReferenceNumber.isPresent()) {
                callbackResponse.addErrors(asList("Sorry, there was a problem submitting your appeal case"));
            } else {
                asylumCase.setAppealReferenceNumber(appealReferenceNumber.get());
            }
        } else {
            callbackResponse.addErrors(asList("Sorry, there was a problem submitting your appeal case"));
        }

        return callbackResponse;
    }
}
