package org.planb.repository;

import org.planb.model.MeetingParticipants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowCallbackHandler; //KI Log 65

import java.util.ArrayList; // KI Log 65
import java.util.HashMap;   //KI Log 65
import java.util.Map;        //KI Log 65
import java.util.List;
import java.util.Optional;
import java.util.Collections;

@Repository
public class JdbcMeetingParticipantsRepository implements MeetingParticipantsRepository {

 private static final String COLUMNS = "user_id, meeting_id";

    private static final RowMapper<MeetingParticipants> MEETING_PARTICIPANTS_MAPPER =
        (rs, rowNum) -> new MeetingParticipants(
            rs.getLong("user_id"),
            rs.getLong("meeting_id")
        );
    
    private final JdbcTemplate jdbcTemplate;

    public JdbcMeetingParticipantsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

@Override 
    public MeetingParticipants save(MeetingParticipants participant){
        String sql = """
            INSERT INTO meeting_participants
                (user_id, meeting_id)
            VALUES (?, ?)
            ON CONFLICT (user_id, meeting_id)
            DO NOTHING
            RETURNING
            """ + COLUMNS;

            List<MeetingParticipants> saved = jdbcTemplate.query(
            sql,
            MEETING_PARTICIPANTS_MAPPER,
            participant.getUserId(),
            participant.getMeetingId()
            );

        if(saved.isEmpty()){
            throw new IllegalStateException("Meeting participant could not be saved");
        }

        return saved.getFirst();
    }

@Override 
    public Optional<MeetingParticipants> findByMeetingIdAndUserId(Long meetingId, Long userId){
        if(meetingId == null || userId ==null){
            return Optional.empty();
        }
        String sql = """
            SELECT %s
            FROM meeting_participants
            WHERE user_id = ?
             AND meeting_id = ?
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, MEETING_PARTICIPANTS_MAPPER, userId, meetingId).stream().findFirst();
    }

@Override 
    public List<MeetingParticipants> findAllByMeetingId(Long meetingId){
        if(meetingId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM meeting_participants
            WHERE meeting_id = ?
            ORDER BY user_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, MEETING_PARTICIPANTS_MAPPER, meetingId);
    }


 @Override 
    public List<MeetingParticipants> findAllByUserId(Long userId){
        if(userId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM meeting_participants
            WHERE user_id = ?
            ORDER BY meeting_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, MEETING_PARTICIPANTS_MAPPER, userId);
    }
    
    @Override 
    public Map<Long, List<Long>> findUserIdsByProjectId(Long projectId){
    if(projectId == null){
        return Map.of();
    }
    String participantSql = """
            SELECT mp.meeting_id, mp.user_id
            FROM meeting_participants mp
            JOIN meetings m ON m.meeting_id = mp.meeting_id
            WHERE m.project_id = ?
            """;
//KI Log 65
    Map<Long, List<Long>> participantsByMeeting = new HashMap<>();
    jdbcTemplate.query(participantSql, (RowCallbackHandler) rs ->
            participantsByMeeting
                    .computeIfAbsent(rs.getLong("meeting_id"), id -> new ArrayList<>())
                    .add(rs.getLong("user_id")),
            projectId);

    return participantsByMeeting;
    }

    @Override 
    public boolean existsByMeetingIdAndUserId(Long meetingId, Long userId){
        if(meetingId == null || userId == null){
            return false;
        }
        String sql = """
            SELECT EXISTS(
                SELECT 1
                FROM meeting_participants
                WHERE user_id = ?
                 AND meeting_id = ?
            ) """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, userId, meetingId);
        return Boolean.TRUE.equals(exists);
    }

     @Override 
    public void deleteByMeetingIdAndUserId(Long meetingId, Long userId){
        if(meetingId == null || userId == null){
            return;
        }
        String sql = """
            DELETE
            FROM meeting_participants
            WHERE user_id = ?
             AND meeting_id = ?
            """;
        jdbcTemplate.update(sql, userId, meetingId);
    }

  @Override 
    public void deleteAllByMeetingId(Long meetingId){
        if(meetingId == null){
            return;
        }
        String sql = """
            DELETE
            FROM meeting_participants
            WHERE meeting_id = ?
            """;
        jdbcTemplate.update(sql, meetingId);
    }
}
