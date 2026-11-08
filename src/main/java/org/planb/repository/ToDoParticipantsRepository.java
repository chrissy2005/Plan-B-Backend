package org.planb.repository;

import java.util.List;
import java.util.Optional;

import org.planb.model.ToDoParticipants;

public interface ToDoParticipantsRepository {

ToDoParticipants save(ToDoParticipants participant);


Optional<ToDoParticipants> findByLongToDoAndUserId(Long longToDoId, Long userId);


List<ToDoParticipants> findAllByLongToDoId(Long longToDoId);


List<ToDoParticipants> findAllByuserId(Long userId);


boolean existsByLongTodoIdAndUserId(Long longTodoId, Long userId);


void deleteByLongTodoIdAndUserId(Long longTodoId, Long userId);


void deleteAllByLongTodoId(Long longTodoId);


}
