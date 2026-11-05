package org.planb.model;

import java.time.OffsetDateTime;

public class DashboardSubtaskEntry {
    private String title;
    private String description;
    private Long subtaskId;
    private Long longToDoId;
    private boolean done;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private TaskPriority priority;

    public DashboardSubtaskEntry(){

    }


  public DashboardSubtaskEntry(String title, String description, Long subtaskId, Long longToDoId, boolean done, OffsetDateTime start, OffsetDateTime end, TaskPriority priority){
    this.title = title;
    this.description = description;
    this.subtaskId = subtaskId;
    this.longToDoId = longToDoId;
    this.done = done;
    this.start = start;
    this.end = end;
    this.priority = priority;
    }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getSubtaskId() {
    return subtaskId;
  }

  public void setSubtaskId(Long subtaskId) {
    this.subtaskId = subtaskId;
  }

  public boolean isDone() {
    return done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  

  public OffsetDateTime getStart() {
    return start;
}


  public void setStart(OffsetDateTime start) {
    this.start = start;
  }


  public OffsetDateTime getEnd() {
    return end;
  }


  public void setEnd(OffsetDateTime end) {
    this.end = end;
  }


  public Long getLongToDoId() {
    return longToDoId;
  }

  public void setLongToDoId(Long longToDoId) {
    this.longToDoId = longToDoId;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  public void setPriority(TaskPriority priority) {
    this.priority = priority;
  }

}
