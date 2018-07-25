package minwiggles.basicstoryline.crossing;


public class CharacterCrossings {

    private int timePoint;
    private double[] crossingVariables;
//    private int c1;
//    private int c2;
//    private int crossing;

    public CharacterCrossings(int timePoint, double[] crossingVariables){
        this.timePoint = timePoint;
        this.crossingVariables = crossingVariables;
    }

    public int getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(int timePoint) {
        this.timePoint = timePoint;
    }

    public double[] getCrossingVariables() {
        return crossingVariables;
    }

    public void setCrossingVariables(double[] crossingVariables) {
        this.crossingVariables = crossingVariables;
    }
}

