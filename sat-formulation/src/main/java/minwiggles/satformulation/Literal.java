package minwiggles.satformulation;

public class Literal {

    private boolean leadingSign;
    private String varName;

    public Literal(boolean leadingSign, String varName){
        this.leadingSign = leadingSign;
        this.varName = varName;
    }

    public boolean getLeadingSign() {
        return leadingSign;
    }

    public void setLeadingSign(boolean leadingSign) {
        this.leadingSign = leadingSign;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String toString(){
        if(leadingSign) return varName;
        else return "-"+varName;
    }
}

