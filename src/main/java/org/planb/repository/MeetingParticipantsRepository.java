package org.planb.repository;

import java.util.List;
import java.util.Optional;
import java.util.Map; //KI Log 65

import org.planb.model.MeetingParticipants;

public interface MeetingParticipantsRepository {

MeetingParticipants save(MeetingParticipants participant);

Optional<MeetingParticipants> findByMeetingIdAndUserId(Long meetingId, Long userId);

List<MeetingParticipants> findAllByMeetingId(Long meetingId);

List<MeetingParticipants> findAllByUserId(Long userId);

Map<Long, List<Long>> findUserIdsByProjectId(Long projectId); //KI Log 65

boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

void deleteAllByMeetingId(Long meetingId);

}