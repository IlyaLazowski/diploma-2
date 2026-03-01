package com.fakel.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ControlSummaryId implements Serializable {

    @Column(name = "control_id")
    private Long controlId;

    @Column(name = "cadet_id")
    private Long cadetId;

    public ControlSummaryId() {}

    public ControlSummaryId(Long controlId, Long cadetId) {
        this.controlId = controlId;
        this.cadetId = cadetId;
    }

    // Геттеры и сеттеры
    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlSummaryId that = (ControlSummaryId) o;
        return Objects.equals(controlId, that.controlId) &&
                Objects.equals(cadetId, that.cadetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(controlId, cadetId);
    }
}