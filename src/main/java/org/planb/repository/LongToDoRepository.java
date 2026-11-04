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
//create new longToDo or update a longToDo

Optional<LongToDo> findById(Long LongToDoId);
//load one specific longToDo

List<LongToDo> findAllByProjectId(Long projectId);
//load all longToDos of one project, simple case that is more common

List<LongToDo> searchByProject(Long projectId, TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to, LongToDoSortField sortField, SortDirection sortDirection);
//this is the filter and sort method to search and order specific requests

boolean existsById(Long longToDoId);
//check if the longToDo exists

void deleteById(Long longTodoId);
//delete longToDo

List<CalendarLongToDoEntry> findAllByParticipantInPeriod(Long userId, LocalDateTime periodStart, LocalDateTime periodEnd);
void deleteByProjectId(Long projectId);

List<LongToDo> searchForDashboard(Long longToDoId,TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to);
}

