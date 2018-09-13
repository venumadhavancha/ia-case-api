package uk.gov.hmcts.reform.iacaseapi.domain.entities;

import java.util.Optional;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Document;

public class WrittenLegalArgument {

    private Optional<Document> document = Optional.empty();
    private Optional<String> comment = Optional.empty();

    public Optional<Document> getDocument() {
        return document;
    }

    public Optional<String> getComment() {
        return comment;
    }

    public void setDocument(Document document) {
        this.document = Optional.ofNullable(document);
    }

    public void setComment(String comment) {
        this.comment = Optional.ofNullable(comment);
    }
}