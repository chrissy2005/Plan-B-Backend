package org.planb.repository;

import java.util.List;
import java.util.Optional;

import org.planb.model.Subtask;
import org.planb.model.SortDirection;
import org.planb.model.SubtaskSortField;
import org.planb.model.TaskPriority;

public interface SubtaskRepository {

Subtask save(Subtask subtask);
//create subtask or update

Optional<Subtask> findById(Long subtaskId);
//find one subtask by its Id

Optional<Subtask> findByIdAndLongToDoId(Long subtaskId, Long longToDoId);
//common case: find one subtask of one longToDo

List<Subtask> findByLongToDoId(Long longToDoId);
//common case: all longToDos of one project

List<Subtask> searchByLongTodo(Long longToDoId, TaskPriority priority, Boolean done, Long assigneeId, SubtaskSortField sortField, SortDirection sortDirection);
//this is the filter and sort method to search and order specific requests

Boolean existsById(Long subtaskId);
//check if the subtask exists

void deleteById(Long subtaskId);
//delete subtask by its ID

void deleteAllByLongToDoId(Long longToDoId);
//delete all subtasks that belong to one longToDo

}
