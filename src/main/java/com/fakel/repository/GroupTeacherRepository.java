    package com.fakel.repository;

    import com.fakel.model.Group;
    import com.fakel.model.GroupTeacher;
    import com.fakel.model.GroupTeacherId;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    import java.util.List;

    @Repository
    public interface GroupTeacherRepository extends JpaRepository<GroupTeacher, GroupTeacherId> {

        // Найти все группы преподавателя
        @Query("SELECT gt.group FROM GroupTeacher gt WHERE gt.teacher.userId = :teacherId")
        List<Group> findGroupsByTeacherId(@Param("teacherId") Long teacherId);
    }