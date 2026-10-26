package org.planb.repository;

import org.planb.model.Meeting;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional; //Ki Log 31
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowCallbackHandler; //KI Log 42

import java.util.ArrayList; //KI Log 42
import java.util.HashMap;   //KI Log 42
import java.util.Map;       //KI Log 42
import java.util.Objects;   //KI Log 42
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMeetingRepository implements MeetingRepository {

private static final String COLUMNS =
"meeting_id, project_id, title, description, starts_at, ends_at, is_completed";

    private static final RowMapper<Meeting> MEETINGS_MAPPER = (rs, rowNum) ->
            new Meeting(
                    rs.getLong("meeting_id"),
                    rs.getLong("project_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getObject("starts_at", LocalDateTime.class),
                    rs.getObject("ends_at", LocalDateTime.class),
                    rs.getBoolean("is_completed")
                    
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcMeetingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

@Transactional //KI log 31

@Override
public Meeting save (Meeting meeting){
 if (meeting.getMeetingId() != null) {
            return updateExistingMeeting(meeting);
}

String sql = """
INSERT INTO meetings (project_id, title, description, starts_at, ends_at, is_completed)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING
                """ + COLUMNS;

Meeting savedMeeting = jdbcTemplate.query(
                sql,
                MEETINGS_MAPPER,
                meeting.getProjectId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getStartDateTime(),
                meeting.getEndDateTime(),
                meeting.isDone()
        ).getFirst(); //KI log 32 (verbesserung)

//KI Log 30, angepasst durch KI Log 42
        List<Long> participantIds = normalizeParticipantIds(meeting.getParticipantIds());
        insertParticipants(savedMeeting.getMeetingId(), participantIds);
        savedMeeting.setParticipantIds(participantIds);
        
        return savedMeeting;
}

//KI Log 30
private void insertParticipants(Long meetingId, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }
        String sql = """
        INSERT INTO meeting_participants (meeting_id, user_id) 
        VALUES (?, ?)
        """;

        List<Object[]> rows = participantIds.stream()
                .map(userId -> new Object[]{meetingId, userId})
                .toList();
        jdbcTemplate.batchUpdate(sql, rows);
    }
//KI Log 42: entfernt doppelte und null-IDs aus der Teilnehmerliste
private static List<Long> normalizeParticipantIds(List<Long> participantIds) {
    if (participantIds == null) {
        return List.of();
    }
    return participantIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
}

private Meeting updateExistingMeeting(Meeting meeting) {
        
        String sql = """
                UPDATE meetings
                SET title = ?, description = ?, starts_at = ?, ends_at = ?, is_completed = ?
                WHERE meeting_id = ?
                RETURNING
                """ + COLUMNS;
        

        Meeting updatedMeeting = jdbcTemplate.query(
                sql,
                MEETINGS_MAPPER,
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getStartDateTime(),
                meeting.getEndDateTime(),
                meeting.isDone(), 
                meeting.getMeetingId()
        ).stream() //KI Log 35
            .findFirst()  //KI Log 35
            .orElseThrow(() -> new IllegalStateException("meeting not found")); //KI Log 35
//KI Log 35, angepasst durch KI Log 42
    if (meeting.getParticipantIds() != null) {
    List<Long> participantIds = normalizeParticipantIds(meeting.getParticipantIds());
    replaceParticipants(meeting.getMeetingId(), participantIds);
    updatedMeeting.setParticipantIds(participantIds);
    } else {

    updatedMeeting.setParticipantIds(jdbcTemplate.queryForList(
            "SELECT user_id FROM meeting_participants WHERE meeting_id = ?",
            Long.class,
            meeting.getMeetingId()));
}
return updatedMeeting;
}

//KI Log 35
private void replaceParticipants(Long meetingId, List<Long> participantIds) {
    jdbcTemplate.update("DELETE FROM meeting_participants WHERE meeting_id = ?", meetingId);
    insertParticipants(meetingId, participantIds);
}

   
@Override
public Optional<Meeting> findById(Long meetingId){
    if (meetingId == null) {
            return Optional.empty();
        }

        String sql = "SELECT " + COLUMNS
                + " FROM meetings WHERE meeting_id = ?";
       
    Optional<Meeting> meeting = jdbcTemplate.query(sql, MEETINGS_MAPPER, meetingId)
                .stream()
                .findFirst();
    
    //KI Log 39: Teilnehmer für Detailansicht nachladen
    meeting.ifPresent(m -> m.setParticipantIds(jdbcTemplate.queryForList(
        "SELECT user_id FROM meeting_participants WHERE meeting_id = ?",
        Long.class,
        meetingId)));
    
    return meeting;
    }

@Override
public List<Meeting> findAllByProjectId(Long projectId){
//KI Log 33 ->Implementierung, angepasst
String sql = 
"SELECT " + COLUMNS + " FROM meetings WHERE project_id = ? ORDER BY starts_at";
    List<Meeting> meetings = jdbcTemplate.query(sql, MEETINGS_MAPPER, projectId);

    //KI Log 42
    if (meetings.isEmpty()) {
        return meetings;
    }

    String participantSql = """
            SELECT mp.meeting_id, mp.user_id
            FROM meeting_participants mp
            JOIN meetings m ON m.meeting_id = mp.meeting_id
            WHERE m.project_id = ?
            """;

    Map<Long, List<Long>> participantsByMeeting = new HashMap<>();
    jdbcTemplate.query(participantSql, (RowCallbackHandler) rs ->
            participantsByMeeting
                    .computeIfAbsent(rs.getLong("meeting_id"), id -> new ArrayList<>())
                    .add(rs.getLong("user_id")),
            projectId);

    for (Meeting meeting : meetings) {
        meeting.setParticipantIds(
                participantsByMeeting.getOrDefault(meeting.getMeetingId(), List.of()));
    }

    return meetings;

}


@Override 
public boolean existsById(Long meetingId){
    if (meetingId == null) {
            return false;
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM meetings WHERE meeting_id = ?)",
                Boolean.class,
                meetingId
                );

        return Boolean.TRUE.equals(exists);
    }

@Override 
public void deleteById(Long meetingId){
     if (meetingId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM meetings WHERE meeting_id = ?", meetingId);
    }



}
