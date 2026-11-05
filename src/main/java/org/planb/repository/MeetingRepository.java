package org.planb.repository;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime; //KI Log 67

import java.time.LocalDateTime;
import org.planb.model.Meeting;
import org.planb.model.CalendarMeetingEntry;

public interface MeetingRepository {

Meeting save (Meeting meeting);

Optional<Meeting> findById(Long meetingId);

List<Meeting> findAllByProjectId(Long projectId);

boolean existsById(Long meetingId);

void deleteById(Long meetingId);

List<CalendarMeetingEntry> findAllByParticipantInPeriod(Long userId, OffsetDateTime periodStart, OffsetDateTime periodEnd); //KI Log 68

void deleteByProjectId(Long projectId);

List<Meeting> searchForDashboard(Long meetingId, Boolean done,LocalDateTime from, LocalDateTime to);
}