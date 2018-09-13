package uk.gov.hmcts.reform.iacaseapi.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;

@Service
public class AppealDeadlineCalculator {

    private final int allowedTimeFromDecisionInMonths;

    public AppealDeadlineCalculator(
        @Value("${limits.allowedTimeFromDecisionInMonths}") int allowedTimeFromDecisionInMonths
    ) {
        this.allowedTimeFromDecisionInMonths = allowedTimeFromDecisionInMonths;
    }

    public Optional<LocalDate> calculate(
        AsylumCase asylumCase
    ) {
        if (Strings.isBlank(asylumCase.getHomeOfficeDecisionDate())) {
            return Optional.empty();
        }

        LocalDate decisionDate =
            LocalDate.parse(
                asylumCase.getHomeOfficeDecisionDate(),
                DateTimeFormatter.ISO_LOCAL_DATE
            );

        return Optional.of(decisionDate.plusMonths(1));
    }
}