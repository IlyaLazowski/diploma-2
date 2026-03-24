package com.fakel.controller;

import com.fakel.dto.*;
import com.fakel.model.Control;
import com.fakel.model.Teacher;
import com.fakel.repository.ControlRepository;
import com.fakel.repository.TeacherRepository;
import com.fakel.service.ControlService;
import com.fakel.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ControlService controlService;

    @Autowired
    private ControlRepository controlRepository;

    @Autowired
    private TeacherRepository teacherRepository;


    @GetMapping("/groups")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<GroupDto> getAllGroups(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") boolean onlyMyGroups) {

        Sort sort = Sort.by("foundationDate").descending()
                .and(Sort.by("number").ascending());
        Pageable pageable = PageRequest.of(page, size, sort);

        return groupService.getGroupsWithFilters(userDetails, year, onlyMyGroups, pageable);
    }


    @GetMapping("/groups/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupDto getGroupById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return groupService.getGroupById(id, userDetails);
    }


    @GetMapping("/groups/number/{number}")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupDto getGroupByNumber(
            @PathVariable String number,
            @AuthenticationPrincipal UserDetails userDetails) {
        return groupService.getGroupByNumber(number, userDetails);
    }


    @GetMapping("/groups/{groupId}/controls")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<ControlDto> getGroupControls(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type) {

        Sort sort = Sort.by("date").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return controlService.getGroupControls(userDetails, groupId, dateFrom, dateTo, type, pageable);
    }


    @GetMapping("/my/controls")
    @PreAuthorize("hasRole('CADET')")
    public Page<ControlDto> getMyControls(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type) {

        Sort sort = Sort.by("date").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return controlService.getCadetControls(userDetails, dateFrom, dateTo, type, pageable);
    }


    @GetMapping("/my/controls/{controlId}/results")
    @PreAuthorize("hasRole('CADET')")
    public Page<ControlResultDto> getMyControlResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by("cadet.user.lastName").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return controlService.getControlResultsForCadet(userDetails, controlId, pageable);
    }


    @GetMapping("/my/controls/{controlId}")
    @PreAuthorize("hasRole('CADET')")
    public ControlDto getMyControlById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId) {

        return controlService.getCadetControlById(userDetails, controlId);
    }


    @GetMapping("/my/controls/{controlId}/full")
    @PreAuthorize("hasRole('CADET')")
    public List<ControlSummaryDto> getMyControlFullResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId) {
        return controlService.getControlFullResults(controlId, userDetails);
    }


    @GetMapping("/groups/{groupId}/controls/{controlId}/full")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ControlSummaryDto> getGroupControlFullResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long controlId) {

        // Можно проверить, что control принадлежит groupId
        return controlService.getControlFullResults(controlId, userDetails);
    }



    @PostMapping("/groups/controls/{controlId}/raw-results")
    @PreAuthorize("hasRole('TEACHER')")
    public void submitRawResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId,
            @Valid @RequestBody SubmitRawResultsRequest request) {

        if (!controlId.equals(request.getControlId())) {
            throw new RuntimeException("ID контроля не совпадает");
        }

        controlService.submitRawResults(userDetails, request);
    }

    @PostMapping("/groups/controls")
    @PreAuthorize("hasRole('TEACHER')")
    public ControlDto createControl(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateControlRequest request) {
        return controlService.createControl(userDetails, request);
    }


    @PutMapping("/groups/controls/{controlId}/results")
    @PreAuthorize("hasRole('TEACHER')")
    public void updateControlResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId,
            @Valid @RequestBody UpdateControlResultsRequest request) {

        if (!controlId.equals(request.getControlId())) {
            throw new RuntimeException("ID контроля не совпадает");
        }

        controlService.updateControlResults(userDetails, request);
    }


    /**
     * GET /api/groups/{groupId}/controls/{controlId}/full-details
     * Получить полные результаты контроля с расширенной информацией
     */
    @GetMapping("/groups/{groupId}/controls/{controlId}/full-details")
    @PreAuthorize("hasRole('TEACHER')")
    public ControlFullDetailsDto getControlFullDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long controlId) {

        // Получаем контроль напрямую через репозиторий
        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        // Проверяем, что контроль принадлежит группе
        if (!control.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Контроль не принадлежит указанной группе");
        }

        // Проверяем доступ преподавателя
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        return controlService.getControlFullDetails(controlId, userDetails);
    }

}