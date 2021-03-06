package uk.gov.hmcts.reform.iacaseapi.domain.entities;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum DocumentTag {

    CASE_ARGUMENT("caseArgument"),
    RESPONDENT_EVIDENCE("respondentEvidence"),
    APPEAL_RESPONSE("appealResponse"),
    APPEAL_SUBMISSION("appealSubmission"),

    @JsonEnumDefaultValue
    NONE("");

    private final String id;

    DocumentTag(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
