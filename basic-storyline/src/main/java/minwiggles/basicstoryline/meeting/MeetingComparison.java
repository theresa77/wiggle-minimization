package minwiggles.basicstoryline.meeting;

import java.util.List;

public class MeetingComparison {

    private List<double[]> meetingComparisons;

    public MeetingComparison(List<double[]> meetingComparisons){
        this.meetingComparisons = meetingComparisons;
    }

    public List<double[]> getMeetingComparisons() {
        return meetingComparisons;
    }
}
