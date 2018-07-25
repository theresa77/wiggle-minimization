package minwiggles.basicstoryline.meeting;


public class MeetingVariables {

    private String name;
    private int id;
    private double minSlot;
    private double maxSlot;
    private int startX;
    private int endX;
    private int minY;
    private int maxY;
    private int umcomprStartX;
    private int umcomprEndX;
    private int umcomprMinY;
    private int umcomprMaxY;
    private int compressedStartT;
    private int compressedEndT;
    private int startT;
    private int endT;

    public MeetingVariables(int id, String name, double minSlot, double maxSlot){
        this.id = id;
        this.name = name;
        this.minSlot = minSlot;
        this.maxSlot = maxSlot;
    }

    public MeetingVariables(int id, String name, double minSlot, double maxSlot, int compressedStartT, int compressedEndT){
        this.id = id;
        this.name = name;
        this.minSlot = minSlot;
        this.maxSlot = maxSlot;
        this.compressedStartT = compressedStartT;
        this.compressedEndT = compressedEndT;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public double getMinSlot() {
        return minSlot;
    }

    public double getMaxSlot() {
        return maxSlot;
    }

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getEndX() {
        return endX;
    }

    public void setEndX(int endX) {
        this.endX = endX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public void setMinSlot(double minSlot) {
        this.minSlot = minSlot;
    }

    public void setMaxSlot(double maxSlot) {
        this.maxSlot = maxSlot;
    }

    public int getCompressedStartT() {
        return compressedStartT;
    }

    public void setCompressedStartT(int compressedStartT) {
        this.compressedStartT = compressedStartT;
    }

    public int getCompressedEndT() {
        return compressedEndT;
    }

    public void setCompressedEndT(int compressedEndT) {
        this.compressedEndT = compressedEndT;
    }

    public int getStartT() {
        return startT;
    }

    public void setStartT(int startT) {
        this.startT = startT;
    }

    public int getEndT() {
        return endT;
    }

    public void setEndT(int endT) {
        this.endT = endT;
    }

    public int getUmcomprStartX() {
        return umcomprStartX;
    }

    public void setUmcomprStartX(int umcomprStartX) {
        this.umcomprStartX = umcomprStartX;
    }

    public int getUmcomprEndX() {
        return umcomprEndX;
    }

    public void setUmcomprEndX(int umcomprEndX) {
        this.umcomprEndX = umcomprEndX;
    }

    public int getUmcomprMinY() {
        return umcomprMinY;
    }

    public void setUmcomprMinY(int umcomprMinY) {
        this.umcomprMinY = umcomprMinY;
    }

    public int getUmcomprMaxY() {
        return umcomprMaxY;
    }

    public void setUmcomprMaxY(int umcomprMaxY) {
        this.umcomprMaxY = umcomprMaxY;
    }
}

