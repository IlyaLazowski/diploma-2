package com.fakel.service;

import com.fakel.model.ControlResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarkCalculatorServiceTest {

    private MarkCalculatorService markCalculatorService;

    @BeforeEach
    void setUp() {
        markCalculatorService = new MarkCalculatorService();
    }

    @Test
    void calculateFinalMark_5_5_5_ShouldReturn10() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)5);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)10, result);
    }

    @Test
    void calculateFinalMark_5_5_4_ShouldReturn9() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)4);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)9, result);
    }

    @Test
    void calculateFinalMark_5_4_4_ShouldReturn8() {
        List<Short> marks = Arrays.asList((short)5, (short)4, (short)4);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)8, result);
    }

    @Test
    void calculateFinalMark_4_4_4_ShouldReturn7() {
        List<Short> marks = Arrays.asList((short)4, (short)4, (short)4);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)7, result);
    }

    @Test
    void calculateFinalMark_5_5_3_ShouldReturn6() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)6, result);
    }

    @Test
    void calculateFinalMark_5_4_3_ShouldReturn6() {
        List<Short> marks = Arrays.asList((short)5, (short)4, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)6, result);
    }

    @Test
    void calculateFinalMark_4_4_3_ShouldReturn6() {
        List<Short> marks = Arrays.asList((short)4, (short)4, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)6, result);
    }

    @Test
    void calculateFinalMark_5_3_3_ShouldReturn5() {
        List<Short> marks = Arrays.asList((short)5, (short)3, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)5, result);
    }

    @Test
    void calculateFinalMark_4_3_3_ShouldReturn5() {
        List<Short> marks = Arrays.asList((short)4, (short)3, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)5, result);
    }

    @Test
    void calculateFinalMark_3_3_3_ShouldReturn4() {
        List<Short> marks = Arrays.asList((short)3, (short)3, (short)3);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)4, result);
    }

    @Test
    void calculateFinalMark_5_5_2_Course1_ShouldReturn4() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)2);
        Integer course = 1;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)4, result);
    }

    @Test
    void calculateFinalMark_5_5_2_Course2_ShouldReturn3() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)3, result);
    }

    @Test
    void calculateFinalMark_4_4_2_Course1_ShouldReturn4() {
        List<Short> marks = Arrays.asList((short)4, (short)4, (short)2);
        Integer course = 1;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)4, result);
    }

    @Test
    void calculateFinalMark_4_4_2_Course2_ShouldReturn3() {
        List<Short> marks = Arrays.asList((short)4, (short)4, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)3, result);
    }

    @Test
    void calculateFinalMark_5_3_2_ShouldReturn3() {
        List<Short> marks = Arrays.asList((short)5, (short)3, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)3, result);
    }

    @Test
    void calculateFinalMark_4_3_2_ShouldReturn3() {
        List<Short> marks = Arrays.asList((short)4, (short)3, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)3, result);
    }

    @Test
    void calculateFinalMark_3_3_2_ShouldReturn3() {
        List<Short> marks = Arrays.asList((short)3, (short)3, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)3, result);
    }

    @Test
    void calculateFinalMark_5_2_2_ShouldReturn2() {
        List<Short> marks = Arrays.asList((short)5, (short)2, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)2, result);
    }

    @Test
    void calculateFinalMark_4_2_2_ShouldReturn2() {
        List<Short> marks = Arrays.asList((short)4, (short)2, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)2, result);
    }

    @Test
    void calculateFinalMark_3_2_2_ShouldReturn2() {
        List<Short> marks = Arrays.asList((short)3, (short)2, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)2, result);
    }

    @Test
    void calculateFinalMark_2_2_2_ShouldReturn1() {
        List<Short> marks = Arrays.asList((short)2, (short)2, (short)2);
        Integer course = 2;

        Short result = markCalculatorService.calculateFinalMark(marks, course);

        assertEquals((short)1, result);
    }

    @Test
    void calculateFinalMark_WithNullMarks_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(null, 2));
    }

    @Test
    void calculateFinalMark_WithEmptyMarks_ShouldThrowException() {
        List<Short> marks = new ArrayList<>();

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_With2Marks_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)4);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_With4Marks_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)4, (short)3, (short)2);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_WithNullMark_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, null, (short)3);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_WithMarkLessThan0_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)4, (short)-1);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_WithMarkGreaterThan5_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)4, (short)6);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMark_WithNullCourse_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)5);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, null));
    }

    @Test
    void calculateFinalMark_WithCourseLessThan1_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)5);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 0));
    }

    @Test
    void calculateFinalMark_WithCourseGreaterThan5_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)5, (short)5);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 6));
    }

    @Test
    void calculateFinalMark_WithInvalidCombination_ShouldThrowException() {
        List<Short> marks = Arrays.asList((short)5, (short)1, (short)1);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMark(marks, 2));
    }

    @Test
    void calculateFinalMarks_WithValidData_ShouldReturnMarks() {
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();

        List<ControlResult> results1 = new ArrayList<>();
        results1.add(createControlResult((short)5));
        results1.add(createControlResult((short)5));
        results1.add(createControlResult((short)5));
        resultsByCadet.put(1L, results1);
        coursesByCadet.put(1L, 2);

        List<ControlResult> results2 = new ArrayList<>();
        results2.add(createControlResult((short)5));
        results2.add(createControlResult((short)4));
        results2.add(createControlResult((short)4));
        resultsByCadet.put(2L, results2);
        coursesByCadet.put(2L, 3);

        Map<Long, Short> result = markCalculatorService.calculateFinalMarks(resultsByCadet, coursesByCadet);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals((short)10, result.get(1L));
        assertEquals((short)8, result.get(2L));
    }

    @Test
    void calculateFinalMarks_WithInvalidData_ShouldSkipInvalidCadets() {
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();

        List<ControlResult> results1 = new ArrayList<>();
        results1.add(createControlResult((short)5));
        results1.add(createControlResult((short)5));
        results1.add(createControlResult((short)5));
        resultsByCadet.put(1L, results1);
        coursesByCadet.put(1L, 2);

        List<ControlResult> results2 = new ArrayList<>();
        results2.add(createControlResult((short)5));
        results2.add(createControlResult((short)4));
        resultsByCadet.put(2L, results2);
        coursesByCadet.put(2L, 2);

        List<ControlResult> results3 = new ArrayList<>();
        results3.add(createControlResult((short)5));
        results3.add(createControlResult((short)4));
        results3.add(createControlResult(null));
        resultsByCadet.put(3L, results3);
        coursesByCadet.put(3L, 2);

        Map<Long, Short> result = markCalculatorService.calculateFinalMarks(resultsByCadet, coursesByCadet);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals((short)10, result.get(1L));
    }

    @Test
    void calculateFinalMarks_WithNullResultsByCadet_ShouldThrowException() {
        Map<Long, Integer> coursesByCadet = new HashMap<>();
        coursesByCadet.put(1L, 2);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMarks(null, coursesByCadet));
    }

    @Test
    void calculateFinalMarks_WithNullCoursesByCadet_ShouldThrowException() {
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        resultsByCadet.put(1L, new ArrayList<>());

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMarks(resultsByCadet, null));
    }

    @Test
    void calculateFinalMarks_WithMissingCourse_ShouldThrowException() {
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();

        List<ControlResult> results = new ArrayList<>();
        results.add(createControlResult((short)5));
        results.add(createControlResult((short)5));
        results.add(createControlResult((short)5));
        resultsByCadet.put(1L, results);

        assertThrows(IllegalArgumentException.class,
                () -> markCalculatorService.calculateFinalMarks(resultsByCadet, coursesByCadet));
    }

    @Test
    void calculateFinalMarkForAbsent_ShouldReturnNull() {
        Short result = markCalculatorService.calculateFinalMarkForAbsent();

        assertNull(result);
    }

    private ControlResult createControlResult(Short mark) {
        ControlResult result = new ControlResult();
        result.setMark(mark);
        return result;
    }
}