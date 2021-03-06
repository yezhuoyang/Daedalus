package Compiler.AST;

import Compiler.Symbol.Type;
import Compiler.Symbol.VariableSymbol;
import Compiler.Utils.Position;

public class VarDeclNode extends DeclNode {
    private TypeNode type;
    private Type typeAfterResolve;
    private ExprNode expr;
    private String identifier;
    private Position position;

    //for IR
    private VariableSymbol variableSymbol;
    private boolean isGlobalVariable = false;
    private boolean isParameterVariable = false;

    public VarDeclNode(TypeNode type, ExprNode expr, String identifier, Position position) {
        super(position);
        this.type = type;
        this.expr = expr;
        this.identifier = identifier;
    }

    public VariableSymbol getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(VariableSymbol variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public boolean isGlobalVariable() {
        return isGlobalVariable;
    }

    public void setGlobalVariable() {
        isGlobalVariable = true;
    }

    public boolean isParameterVariable() {
        return isParameterVariable;
    }

    public void setParameterVariable() {
        isParameterVariable = true;
    }

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ExprNode getExpr() {
        return expr;
    }

    public void setExpr(ExprNode expr)  {
        this.expr = expr;
    }

    public Type getTypeAfterResolve() {
        return typeAfterResolve;
    }

    public void setTypeAfterResolve(Type typeAfterResolve) {
        this.typeAfterResolve = typeAfterResolve;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
