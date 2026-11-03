package org.planb.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateMeetingDoneRequest {
    @NotNull
    @Valid 
    private UpdateField update;

    public UpdateField getUpdate(){
        return update;
    }

    public void setUpdate(UpdateField update){
        this.update = update;
    }

    public static class UpdateField {
        @NotBlank 
        private String attribute;
        @NotNull 
        private Boolean newValue;

        public Boolean getNewValue(){
            return newValue;
        }
        public void setNewValue(Boolean newValue){
            this.newValue = newValue;
        }
        public String getAttribute() {
            return attribute;
        }
        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }
    }
}
