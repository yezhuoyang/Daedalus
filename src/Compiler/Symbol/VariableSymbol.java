package Compiler.Symbol;

import Compiler.AST.VarDeclNode;
import Compiler.IR.Operand.Storage;

public class VariableSymbol extends Symbol {
    //for IR
    private Storage variableStorage;
    private int offset;

    public VariableSymbol(String name, Type type, VarDeclNode varDeclNode) {
        super(name, type, varDeclNode);
    }

    @Override
    public boolean isVariableSymbol() {
        return true;
    }

    public Storage getVariableStorage() {
        return variableStorage;
    }

    public void setVariableStorage(Storage variableStorage) {
        this.variableStorage = variableStorage;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
