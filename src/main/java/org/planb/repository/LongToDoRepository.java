package org.planb.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.planb.model.LongToDo;
import org.planb.model.TaskPriority;
import org.planb.model.LongToDoSortField;
import org.planb.model.SortDirection;
import org.planb.model.CalendarLongToDoEntry; //KI Log 69

public interface LongToDoRepository {

LongToDo save(LongToDo longToDo);


Optional<LongToDo> findById(Long LongToDoId);


List<LongToDo> findAllByProjectId(Long projectId);


List<LongToDo> searchByProject(Long projectId, TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to, LongToDoSortField sortField, SortDirection sortDirection);


boolean existsById(Long longToDoId);


void deleteById(Long longTodoId);


List<CalendarLongToDoEntry> findAllByParticipantInPeriod(Long userId, LocalDateTime periodStart, LocalDateTime periodEnd);
void deleteByProjectId(Long projectId);

List<LongToDo> searchForDashboard(Long longToDoId,TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to);
}

