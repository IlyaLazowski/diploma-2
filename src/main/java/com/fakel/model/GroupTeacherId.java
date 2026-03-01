package com.fakel.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GroupTeacherId implements Serializable {

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "group_id")
    private Long groupId;

    public GroupTeacherId() {}

    public GroupTeacherId(Long teacherId, Long groupId) {
        this.teacherId = teacherId;
        this.groupId = groupId;
    }

    // Геттеры и сеттеры
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupTeacherId that = (GroupTeacherId) o;
        return Objects.equals(teacherId, that.teacherId) &&
                Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, groupId);
    }
}