package org.planb.repository;

import java.util.List;
import java.util.Optional;

import org.planb.model.ToDoParticipants;

public interface ToDoParticipantsRepository {

ToDoParticipants save(ToDoParticipants participant);
//create/save/update relatiom

Optional<ToDoParticipants> findByLongToDoAndUserId(Long longToDoId, Long userId);
//find one specific relation

List<ToDoParticipants> findAllByLongToDoId(Long longToDoId);
//find all relations for one longToDo

List<ToDoParticipants> findAllByuserId(Long userId);
//find all relations for one user

boolean existsByLongTodoIdAndUserId(Long longTodoId, Long userId);
//check if relation exists

void deleteByLongTodoIdAndUserId(Long longTodoId, Long userId);
//delete one relation

void deleteAllByLongTodoId(Long longTodoId);
//delete all relations of one longToDo

}
