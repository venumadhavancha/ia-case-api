package uk.gov.hmcts.reform.iacaseapi.infrastructure;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.AppealReferenceNumber;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.AsylumAppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.service.AppealReferenceNumberInitializer;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.clients.CoreCaseDataRetriever;

@Service
public class AppealReferenceNumberInitializerFromCcd implements AppealReferenceNumberInitializer {

    private final CoreCaseDataRetriever coreCaseDataRetriever;
    private final SystemDateProvider systemDateProvider;
    private final String appealReferenceSequenceSeed;
    private final Map<AsylumAppealType, AppealReferenceNumber> lastAppealReferenceNumbers = new HashMap<>();

    public AppealReferenceNumberInitializerFromCcd(
            CoreCaseDataRetriever coreCaseDataRetriever,
            SystemDateProvider systemDateProvider,
            @Value("${appealReferenceSequenceSeed}") String appealReferenceSequenceSeed
    ) {
        this.coreCaseDataRetriever = coreCaseDataRetriever;
        this.systemDateProvider = systemDateProvider;
        this.appealReferenceSequenceSeed = appealReferenceSequenceSeed;
    }

    public synchronized Map<AsylumAppealType, AppealReferenceNumber> initialize() {

        if (lastAppealReferenceNumbers.isEmpty()) {

            List<Map> asylumCases = coreCaseDataRetriever.retrieveAppealCasesInAllStatesExceptAppealStarted();

            List<Map> filteredAsylumCases = removeCasesWithoutAnAppealReferenceNumber(asylumCases);

            if (filteredAsylumCases.isEmpty()) {
                initializeFromSeed();
            } else {
                initializeFromExistingAsylumCases(filteredAsylumCases);
            }
        }

        return lastAppealReferenceNumbers;
    }

    private void initializeFromExistingAsylumCases(List<Map> asylumCases) {
        asylumCases.stream()
                .map(this::extractReferenceNumber)
                .collect(groupingBy(AppealReferenceNumber::getType))
                .forEach((key, appealReferenceNumbers) -> {
                    appealReferenceNumbers.sort(new AppealReferenceNumberComparator());
                    lastAppealReferenceNumbers.put(key, appealReferenceNumbers.get(0));
                });
    }

    private void initializeFromSeed() {
        stream(AsylumAppealType.values())
                .forEach(asylumAppealType -> lastAppealReferenceNumbers.put(
                        asylumAppealType, new AppealReferenceNumber(
                                asylumAppealType,
                                appealReferenceSequenceSeed,
                                String.valueOf(systemDateProvider.now().getYear()))));
    }

    private List<Map> removeCasesWithoutAnAppealReferenceNumber(List<Map> asylumCases) {
        return asylumCases.stream()
                .filter(map -> map.get("case_data") instanceof Map)
                .filter(map -> ((Map) map.get("case_data")).get("appealReferenceNumber") != null)
                .collect(toList());
    }

    private AppealReferenceNumber extractReferenceNumber(Map appealMap) {
        String appealReferenceNumber =
                (String) ((Map)appealMap.get("case_data")).get("appealReferenceNumber");

        return new AppealReferenceNumber(appealReferenceNumber);
    }


}