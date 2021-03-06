package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.iacaseapi.domain.UserDetailsProvider;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.UserDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class LegalRepresentativeDetailsHandlerTest {

    @Mock private UserDetailsProvider userDetailsProvider;
    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;
    @Mock private UserDetails userDetails;

    private LegalRepresentativeDetailsHandler legalRepresentativeDetailsHandler;

    @Before
    public void setUp() {

        legalRepresentativeDetailsHandler =
            new LegalRepresentativeDetailsHandler(userDetailsProvider);
    }

    @Test
    public void should_set_legal_representative_details_into_the_case() {

        final String expectedLegalRepresentativeName = "John Doe";
        final String expectedLegalRepresentativeEmailAddress = "john.doe@example.com";

        when(userDetails.getForename()).thenReturn("John");
        when(userDetails.getSurname()).thenReturn("Doe");
        when(userDetails.getEmailAddress()).thenReturn(expectedLegalRepresentativeEmailAddress);
        when(userDetailsProvider.getUserDetails()).thenReturn(userDetails);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(Event.SUBMIT_APPEAL);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.getLegalRepresentativeName()).thenReturn(Optional.empty());
        when(asylumCase.getLegalRepresentativeEmailAddress()).thenReturn(Optional.empty());

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            legalRepresentativeDetailsHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());
        verify(asylumCase, times(1)).setLegalRepresentativeName(expectedLegalRepresentativeName);
        verify(asylumCase, times(1)).setLegalRepresentativeEmailAddress(expectedLegalRepresentativeEmailAddress);
    }

    @Test
    public void should_not_overwrite_existing_legal_representative_details() {

        when(userDetailsProvider.getUserDetails()).thenReturn(userDetails);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(Event.SUBMIT_APPEAL);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.getLegalRepresentativeName()).thenReturn(Optional.of("existing"));
        when(asylumCase.getLegalRepresentativeEmailAddress()).thenReturn(Optional.of("existing"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            legalRepresentativeDetailsHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());
        verify(asylumCase, never()).setLegalRepresentativeName(any());
        verify(asylumCase, never()).setLegalRepresentativeEmailAddress(any());
    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);

        when(callback.getEvent()).thenReturn(Event.SEND_DIRECTION);
        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = legalRepresentativeDetailsHandler.canHandle(callbackStage, callback);

                if (event == Event.SUBMIT_APPEAL
                    && callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> legalRepresentativeDetailsHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
