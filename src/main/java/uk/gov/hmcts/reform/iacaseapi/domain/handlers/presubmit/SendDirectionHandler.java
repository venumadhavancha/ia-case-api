package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.Direction;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DirectionTag;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.Parties;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.DirectionAppender;

@Component
public class SendDirectionHandler implements PreSubmitCallbackHandler<AsylumCase> {

    private final DirectionAppender directionAppender;

    public SendDirectionHandler(
        DirectionAppender directionAppender
    ) {
        this.directionAppender = directionAppender;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT
               && callback.getEvent() == Event.SEND_DIRECTION;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

        String sendDirectionExplanation =
            asylumCase
                .getSendDirectionExplanation()
                .orElseThrow(() -> new IllegalStateException("sendDirectionExplanation is not present"));

        Parties sendDirectionParties =
            asylumCase
                .getSendDirectionParties()
                .orElseThrow(() -> new IllegalStateException("sendDirectionParties is not present"));

        String sendDirectionDateDue =
            asylumCase
                .getSendDirectionDateDue()
                .orElseThrow(() -> new IllegalStateException("sendDirectionDateDue is not present"));

        final List<IdValue<Direction>> existingDirections =
            asylumCase
                .getDirections()
                .orElse(Collections.emptyList());

        List<IdValue<Direction>> allDirections =
            directionAppender.append(
                existingDirections,
                sendDirectionExplanation,
                sendDirectionParties,
                sendDirectionDateDue,
                DirectionTag.NONE
            );

        asylumCase.setDirections(allDirections);

        asylumCase.clearSendDirectionExplanation();
        asylumCase.clearSendDirectionParties();
        asylumCase.clearSendDirectionDateDue();

        return new PreSubmitCallbackResponse<>(asylumCase);
    }
}
