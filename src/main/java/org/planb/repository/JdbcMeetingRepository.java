package org.planb.repository;

import org.planb.model.Meeting;
import org.planb.model.CalendarMeetingEntry;
import java.time.LocalDateTime;
import java.time.OffsetDateTime; //KI Log 67
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.Timestamp;

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
                    //rs.getObject("starts_at", LocalDateTime.class),
                    //rs.getObject("ends_at", LocalDateTime.class),
                    rs.getTimestamp("starts_at").toLocalDateTime(),
                    rs.getTimestamp("ends_at").toLocalDateTime(),
                    rs.getBoolean("is_completed")
                    
            );

    //KI Log 67
    private static final RowMapper<CalendarMeetingEntry> CALENDAR_MEETING_MAPPER = (rs, rowNum) ->
            new CalendarMeetingEntry(
                    rs.getLong("meeting_id"),
                    rs.getString("title"),
                    rs.getString("project_name"),
                    rs.getObject("starts_at", OffsetDateTime.class),
                    rs.getObject("ends_at", OffsetDateTime.class),
                    rs.getBoolean("is_completed")
            ); 

    private final JdbcTemplate jdbcTemplate;

    public JdbcMeetingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
        ).getFirst(); //KI Log 64

        
        return savedMeeting;
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
        ).stream() //KI Log 66
            .findFirst()  //KI Log 66
            .orElseThrow(() -> new IllegalStateException("meeting not found")); //KI Log 66
            
        return updatedMeeting;
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
    
    return meeting;
    }

@Override
public List<Meeting> findAllByProjectId(Long projectId){
//KI Log 65
String sql = 
"SELECT " + COLUMNS + " FROM meetings WHERE project_id = ? ORDER BY starts_at";
    List<Meeting> meetings = jdbcTemplate.query(sql, MEETINGS_MAPPER, projectId);

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

public void deleteByProjectId(Long projectId){
     if (projectId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM meetings WHERE project_id = ?", projectId);
    }

@Override 
public List<CalendarMeetingEntry> findAllByParticipantInPeriod(Long userId, OffsetDateTime periodStart, OffsetDateTime periodEnd){ //KI Log 68
    //Ki Log 68 
    String sql = """
            SELECT m.meeting_id, m.title, p.name AS project_name, m.starts_at, m.ends_at, m.is_completed
            FROM meetings m
            JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id
            JOIN projects p ON m.project_id = p.project_id
            WHERE mp.user_id = ?  AND m.ends_at > ? AND m.starts_at < ?
            ORDER BY m.starts_at
            """;

    List<CalendarMeetingEntry> meetings = jdbcTemplate.query(
            sql,
            CALENDAR_MEETING_MAPPER,
            userId,
            periodStart,
            periodEnd
    );

    return meetings;
}
public List<Meeting> searchForDashboard(Long meetingId, Boolean done, LocalDateTime from, LocalDateTime to) {
    if(meetingId == null){
        return Collections.emptyList();
    }    
    StringBuilder sql = new StringBuilder("""
            SELECT %s
            FROM meetings
            WHERE meeting_id = ?
            """.formatted(COLUMNS));
        
        List<Object> parameters = new ArrayList<>();
        parameters.add(meetingId);

        if (done != null) {
            sql.append(" AND is_completed = ?");
            parameters.add(done);
        }
        if (from != null) {
            sql.append(" AND starts_at >= ?");
            parameters.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND ends_at <= ?");
            parameters.add(Timestamp.valueOf(to));
        }

        sql.append(" ORDER BY starts_at ASC");
        return jdbcTemplate.query(sql.toString(), MEETINGS_MAPPER, parameters.toArray());
    }
}
