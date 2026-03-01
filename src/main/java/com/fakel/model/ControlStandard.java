package com.fakel.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "control_standards")
public class ControlStandard {

    @EmbeddedId
    private ControlStandardId id;

    @ManyToOne
    @MapsId("controlId")
    @JoinColumn(name = "control_id")
    private Control control;

    // Храним ТОЛЬКО номер норматива
    // private Short standardNumber; - это в ID

    public ControlStandard() {}

    public ControlStandard(Control control, Short standardNumber) {
        this.control = control;
        this.id = new ControlStandardId(control.getId(), standardNumber);
    }

    public ControlStandardId getId() { return id; }
    public void setId(ControlStandardId id) { this.id = id; }

    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }

    public Short getStandardNumber() {
        return id != null ? id.getStandardNumber() : null;
    }

    @Embeddable
    public static class ControlStandardId implements Serializable {

        @Column(name = "control_id")
        private Long controlId;

        @Column(name = "standard_number")  // ← храним ТОЛЬКО номер
        private Short standardNumber;

        public ControlStandardId() {}

        public ControlStandardId(Long controlId, Short standardNumber) {
            this.controlId = controlId;
            this.standardNumber = standardNumber;
        }

        public Long getControlId() { return controlId; }
        public void setControlId(Long controlId) { this.controlId = controlId; }

        public Short getStandardNumber() { return standardNumber; }
        public void setStandardNumber(Short standardNumber) { this.standardNumber = standardNumber; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ControlStandardId that = (ControlStandardId) o;
            return Objects.equals(controlId, that.controlId) &&
                    Objects.equals(standardNumber, that.standardNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(controlId, standardNumber);
        }
    }
}