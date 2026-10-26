package org.planb.repository;

import java.util.List;
import java.util.Optional;

import org.planb.model.Meeting;

public interface MeetingRepository {

Meeting save (Meeting meeting);

Optional<Meeting> findById(Long meetingId);

List<Meeting> findAllByProjectId(Long projectId);

boolean existsById(Long meetingId);

void deleteById(Long meetingId);

}