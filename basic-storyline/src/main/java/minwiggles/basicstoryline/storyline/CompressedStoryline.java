package minwiggles.basicstoryline.storyline;

public class CompressedStoryline {

    private String[][] storyline;
    private double obj;

    public CompressedStoryline(String[][] storyline, double obj){
        this.storyline = storyline;
        this.obj = obj;
    }

    public String[][] getStoryline() {
        return storyline;
    }

    public void setStoryline(String[][] storyline) {
        this.storyline = storyline;
    }

    public double getObj() {
        return obj;
    }

    public void setObj(double obj) {
        this.obj = obj;
    }
}
