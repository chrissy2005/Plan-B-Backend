package org.planb.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.planb.model.Subtask;
import org.planb.model.SortDirection;
import org.planb.model.SubtaskSortField;
import org.planb.model.TaskPriority;
import org.planb.model.CalendarSubtaskEntry;
import org.planb.model.DashboardSubtaskEntry;

import java.time.OffsetDateTime;

public interface SubtaskRepository {

Subtask save(Subtask subtask);


Optional<Subtask> findById(Long subtaskId);


Optional<Subtask> findByIdAndLongToDoId(Long subtaskId, Long longToDoId);


List<Subtask> findByLongToDoId(Long longToDoId);


List<Subtask> searchByLongTodo(Long longToDoId, TaskPriority priority, Boolean done, Long assigneeId, SubtaskSortField sortField, SortDirection sortDirection);


Boolean existsById(Long subtaskId);


int claimIfUnassigned(Long subtaskId, Long userId);

int releaseIfAssignedTo(Long subtaskId, Long userId);

void deleteById(Long subtaskId);


void deleteAllByLongToDoId(Long longToDoId);


List<CalendarSubtaskEntry> findAllByAssigneeInPeriod(Long userId, OffsetDateTime periodStart, OffsetDateTime periodEnd);

List<DashboardSubtaskEntry> searchForDashboard(Long longToDoId, TaskPriority priority, Boolean done, Long assigneeId, OffsetDateTime from, OffsetDateTime to);

int updateScheduleIfAssignedTo(Long longToDoId, Long subtaskId, Long userId, OffsetDateTime startDateTime, OffsetDateTime endDateTime);
}
