package com.fakel.controller;
import com.fakel.dto.*;
import jakarta.validation.Valid;  // для @Valid
import com.fakel.service.ControlService;
import com.fakel.service.GroupService;
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

    // ============= ГРУППЫ (ТОЛЬКО ДЛЯ ПРЕПОДАВАТЕЛЯ) =============

    /**
     * GET /api/groups?page=0&size=10&year=2023&onlyMyGroups=true
     */
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

    /**
     * GET /api/groups/{id}
     */
    @GetMapping("/groups/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupDto getGroupById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return groupService.getGroupById(id, userDetails);
    }

    /**
     * GET /api/groups/number/{number}
     */
    @GetMapping("/groups/number/{number}")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupDto getGroupByNumber(
            @PathVariable String number,
            @AuthenticationPrincipal UserDetails userDetails) {
        return groupService.getGroupByNumber(number, userDetails);
    }

    // ============= КОНТРОЛИ ГРУППЫ (ТОЛЬКО ДЛЯ ПРЕПОДАВАТЕЛЯ) =============

    /**
     * GET /api/groups/{groupId}/controls?page=0&size=10&dateFrom=2026-01-01&dateTo=2026-12-31&type=Экзамен
     */
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

    // ============= МОИ КОНТРОЛИ (ТОЛЬКО ДЛЯ КУРСАНТА) =============

    /**
     * GET /api/my/controls?page=0&size=10&dateFrom=2026-01-01&dateTo=2026-12-31&type=Экзамен
     */
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

    /**
     * GET /api/my/controls/{controlId}/results?page=0&size=10
     * Результаты контроля для курсанта (вся группа)
     */
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

    /**
     * GET /api/my/controls/{controlId}
     */
    @GetMapping("/my/controls/{controlId}")
    @PreAuthorize("hasRole('CADET')")
    public ControlDto getMyControlById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId) {

        return controlService.getCadetControlById(userDetails, controlId);
    }

    /**
     * GET /api/my/controls/{controlId}/full
     * Получить полные результаты контроля для курсанта (вся группа)
     */
    @GetMapping("/my/controls/{controlId}/full")
    @PreAuthorize("hasRole('CADET')")
    public List<ControlSummaryDto> getMyControlFullResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long controlId) {
        return controlService.getControlFullResults(controlId, userDetails);
    }

    /**
     * GET /api/groups/{groupId}/controls/{controlId}/full
     * Получить полные результаты контроля для преподавателя
     */
    @GetMapping("/groups/{groupId}/controls/{controlId}/full")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ControlSummaryDto> getGroupControlFullResults(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long controlId) {

        // Можно проверить, что control принадлежит groupId
        return controlService.getControlFullResults(controlId, userDetails);
    }


    /**
     * POST /api/groups/controls/{controlId}/raw-results
     * Отправить сырые результаты контроля
     */
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


}