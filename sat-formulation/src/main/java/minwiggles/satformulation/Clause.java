package minwiggles.satformulation;

import java.util.ArrayList;
import java.util.List;

public class Clause {

    List<Literal> literalList;

    public Clause(){
        literalList = new ArrayList<>();
    }

    public List<Literal> getLiteralList() {
        return literalList;
    }

    public void setLiteralList(List<Literal> literalList) {
        this.literalList = literalList;
    }

    public void addLiteral(Literal literal){
        literalList.add(literal);
    }

    public void removeLiteral(String varName){
        List<Literal> literalListNew = new ArrayList<>();
        for(Literal literal: literalList){
            if(!literal.getVarName().equals(varName))
                literalListNew.add(literal);
        }
        literalList = literalListNew;
    }

    public String toString(){
        String clauseAsString = "";
        for(int l=0; l<literalList.size(); l++) {
            if (l == 0) {
                clauseAsString += literalList.get(l).toString();
            } else {
                clauseAsString += " "+literalList.get(l).toString();
            }
        }
        clauseAsString += " 0";
        return clauseAsString;
    }
}
