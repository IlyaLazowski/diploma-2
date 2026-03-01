package com.fakel.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "control_summaries")
public class ControlSummary {

    @EmbeddedId
    private ControlSummaryId id;

    @ManyToOne
    @MapsId("controlId")
    @JoinColumn(name = "control_id")
    private Control control;

    @ManyToOne
    @MapsId("cadetId")
    @JoinColumn(name = "cadet_id")
    private Cadet cadet;

    @Column(name = "cadet_military_rank")
    private String cadetMilitaryRank;

    @Column(name = "cadet_post")
    private String cadetPost;

    @Column(name = "final_mark")
    private Short finalMark;

    // Геттеры и сеттеры
    public ControlSummaryId getId() { return id; }
    public void setId(ControlSummaryId id) { this.id = id; }

    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }

    public Cadet getCadet() { return cadet; }
    public void setCadet(Cadet cadet) { this.cadet = cadet; }

    public String getCadetMilitaryRank() { return cadetMilitaryRank; }
    public void setCadetMilitaryRank(String cadetMilitaryRank) { this.cadetMilitaryRank = cadetMilitaryRank; }

    public String getCadetPost() { return cadetPost; }
    public void setCadetPost(String cadetPost) { this.cadetPost = cadetPost; }

    public Short getFinalMark() { return finalMark; }
    public void setFinalMark(Short finalMark) { this.finalMark = finalMark; }
}