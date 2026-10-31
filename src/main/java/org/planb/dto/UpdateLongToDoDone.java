package org.planb.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateLongToDoDone {
    @NotNull 
    private Boolean done;

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}
