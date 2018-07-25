package minwiggles.basicstoryline.meeting;


public class MeetingsInformation {

    private MeetingVariables[] meetingVariables;

    public MeetingsInformation(MeetingVariables[] meetingVariables){
        this.meetingVariables = meetingVariables;
    }

    public MeetingVariables[] getMeetingVariables() {
        return meetingVariables;
    }
}
