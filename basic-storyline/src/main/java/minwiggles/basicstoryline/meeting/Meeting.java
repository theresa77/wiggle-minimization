package minwiggles.basicstoryline.meeting;


public class Meeting {

    private String name;
    private int id;
    private int startTP;
    private int endTP;
    private int memberCount;
    private int[] members;

    public Meeting(int id, String name, int startTP, int endTP, int[] members){
        this.id = id;
        this.name = name;
        this.startTP = startTP;
        this.endTP = endTP;
        this.members = members;
        this.memberCount = members.length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStartTP() {
        return startTP;
    }

    public void setStartTP(int startTP) {
        this.startTP = startTP;
    }

    public int getEndTP() {
        return endTP;
    }

    public void setEndTP(int endTP) {
        this.endTP = endTP;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int[] getMembers() {
        return members;
    }

    public void setMembers(int[] members) {
        this.members = members;
    }

    public int getTimePointsCount(){
        return endTP-startTP;
    }
}