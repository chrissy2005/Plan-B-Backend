package org.planb.model;

public class Subtask {
    private String title;
    private String description;
    private Long subtaskId;
    private Long longToDoId;
    private boolean done = false;
    private Long assigneeId;
    private TaskPriority priority;

    public Subtask(){

    }

  public Subtask(String title, String description, Long subtaskId, Long longToDoId, boolean done, Long assignee_id, TaskPriority priority){
    this.title = title;
    this.description = description;
    this.subtaskId = subtaskId;
    this.longToDoId = longToDoId;
    this.done = done;
    this.assigneeId = assignee_id;
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

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long assignee_id) {
    this.assigneeId = assignee_id;
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
