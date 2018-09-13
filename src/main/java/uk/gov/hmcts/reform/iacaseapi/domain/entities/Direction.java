package uk.gov.hmcts.reform.iacaseapi.domain.entities;

import java.util.Optional;

public class Direction {

    private Optional<String> direction = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Optional<String> parties = Optional.empty();
    private Optional<String> dueDate = Optional.empty();

    public Optional<String> getDirection() {
        return direction;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Optional<String> getParties() {
        return parties;
    }

    public Optional<String> getDueDate() {
        return dueDate;
    }

    public void setDirection(String direction) {
        this.direction = Optional.ofNullable(direction);
    }

    public void setDescription(String description) {
        this.description = Optional.ofNullable(description);
    }

    public void setParties(String parties) {
        this.parties = Optional.ofNullable(parties);
    }

    public void setDueDate(String dueDate) {
        this.dueDate = Optional.ofNullable(dueDate);
    }
}