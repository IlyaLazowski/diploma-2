package com.fakel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "group_teachers")
public class GroupTeacher {

    @EmbeddedId
    private GroupTeacherId id;

    @ManyToOne
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    // Конструкторы
    public GroupTeacher() {}

    public GroupTeacher(Teacher teacher, Group group) {
        this.teacher = teacher;
        this.group = group;
        this.id = new GroupTeacherId(teacher.getUserId(), group.getId());
    }

    // Геттеры и сеттеры
    public GroupTeacherId getId() { return id; }
    public void setId(GroupTeacherId id) { this.id = id; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
}