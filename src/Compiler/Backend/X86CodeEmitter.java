package Compiler.Backend;

import Compiler.IR.BasicBlock;
import Compiler.IR.Function;
import Compiler.IR.IRRoot;
import Compiler.IR.IRVisitor;
import Compiler.IR.Instruction.*;
import Compiler.IR.Operand.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static Compiler.IR.Operand.PhysicalRegister.rbp;
import static Compiler.IR.Operand.PhysicalRegister.rsp;

public class X86CodeEmitter implements IRVisitor {
    private IRRoot irRoot;
    private PrintStream out;
    private String indent = "        ";
    private Map<Storage, String> storageStringMap = new HashMap<>();
    private Map<BasicBlock, String> basicBlockStringMap = new HashMap<>();
    private Map<String, Integer> nameCountMap = new HashMap<>();

    public X86CodeEmitter(IRRoot irRoot, PrintStream out) {
        this.irRoot = irRoot;
        this.out = out;
    }

    public void run() {
        irRoot.getFunctionMap().values().forEach(this::constructStackFrame);
        visit(irRoot);
    }

    private void constructStackFrame(Function function) {
        BasicBlock entryBB = function.getEntryBlock();
        BasicBlock exitBB = function.getExitBlock();
        int stackFrameSize = (Math.max(function.argumentLimit - 6, 0) + function.temporaryCnt) * 8;

        if (stackFrameSize != 0)
            entryBB.head.prependInstruction(new Binary(entryBB, Binary.Op.SUB, rsp, new Immediate(stackFrameSize), rsp));
        entryBB.head.prependInstruction(new Move(entryBB, rsp, rbp));
        entryBB.head.prependInstruction(new Push(entryBB, rbp));

        if (stackFrameSize != 0)
            exitBB.tail.prependInstruction(new Binary(exitBB, Binary.Op.ADD, rsp, new Immediate(stackFrameSize), rsp));
        exitBB.tail.prependInstruction(new Pop(exitBB, rbp));
    }

    private void printLabel(String msg) {
        out.println(msg);
    }

    private void printInstruction(String msg) {
        out.println(indent + msg);
    }

    private String staticStringSequntialize(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, n = str.length(); i < n; ++i) {
            char c = str.charAt(i);
            stringBuilder.append((int) c);
            stringBuilder.append(", ");
        }
        stringBuilder.append(0);
        return stringBuilder.toString();
    }

    @Override
    public void visit(IRRoot irRoot) {
        printLabel("default rel");
        printLabel("");
        printLabel("global __init_entry_1");
        irRoot.getGlobalVariableList().forEach(globalVariable -> printLabel("global " + getName((GlobalI64Value) globalVariable)));
        irRoot.getStaticStringList().forEach(staticString -> printLabel("global" + getName(staticString.getBase())));
        printLabel("");
        printLabel("extern printf, scanf, puts, gets, sprintf, sscanf, getchar, strlen, strcmp, strcpy, strncpy, malloc");
        if (!irRoot.getGlobalVariableList().isEmpty()) {
            printLabel("");
            printLabel("SECTION .DATA_common align=8 noexecute");
            printLabel("");
            irRoot.getGlobalVariableList().forEach(globalVariable -> printLabel(getName((GlobalI64Value) globalVariable) + ": dq 0"));
        }
        if (!irRoot.getGlobalVariableList().isEmpty()) {
            printLabel("");
            printLabel("SECTION .DATA_cstring align=1 noexecute");
            printLabel("");
            irRoot.getStaticStringList().forEach(staticString -> {
                printLabel(getName(staticString.getBase()) + ":");
                printInstruction("dq " + staticString.getVal().length());
                printInstruction("db " + staticStringSequntialize(staticString.getVal()) + " ; " + StringEscapeUtils.escapeJava(staticString.getVal()));
            });
        }
        printLabel("");
        printLabel("SECTION .TEXT align=16 execute");
        printLabel("");
        for (Map.Entry<String, Function> entry : irRoot.getFunctionMap().entrySet()) {
            entry.getValue().accept(this);
            printLabel("");
            printLabel("ALIGN   16");
            printLabel("");
        }
    }

    @Override
    public void visit(Function function) {
        function.getReversePostOrderDFSBBList().forEach(this::visit);
    }

    @Override
    public void visit(StaticString staticString) {

    }

    @Override
    public void visit(BasicBlock basicBlock) {
        printLabel(getLabel(basicBlock) + ":");
        for (IRInstruction irInstruction = basicBlock.head; irInstruction != null; irInstruction = irInstruction.getNextInstruction())
            irInstruction.accept(this);
        printLabel("");
    }

    @Override
    public void visit(Alloc inst) {
        printInstruction("call malloc");
    }

    @Override
    public void visit(Binary inst) {
        String op = null;
        switch (inst.getOp()) {
            case DIV:
            case MOD: {
                printInstruction("cdq");
                printInstruction("idiv rcx");
                return;
            }
            case MUL:
                op = "imul";
                break;
            case SHR:
                op = "sar";
                if (!(inst.getSrc2() instanceof Immediate)) {
                    out.print(indent + op + " ");
                    inst.getDst().accept(this);
                    out.println(", cl");
                    return;
                }
                break;
            case SHL:
                op = "shl";
                if (!(inst.getSrc2() instanceof Immediate)) {
                    out.print(indent + op + " ");
                    inst.getDst().accept(this);
                    out.println(", cl");
                    return;
                }
                break;
            case SUB:
                op = "sub";
                break;
            case ADD:
                op = "add";
                break;
            case XOR:
                op = "xor";
                break;
            case AND:
                op = "and";
                break;
            case OR:
                op = "or";
                break;
        }
        out.print(indent + op + " ");
        inst.getSrc1().accept(this);
        out.print(", ");
        inst.getSrc2().accept(this);
        out.println();
    }

    @Override
    public void visit(Branch inst) {
        String op = null;
        switch (inst.defOfCond.getOp()) {
            case LT:
                op = "jl";
                break;
            case LEQ:
                op = "jle";
                break;
            case EQ:
                op = "jeq";
                break;
            case GEQ:
                op = "jge";
                break;
            case GT:
                op = "jg";
                break;
            case NEQ:
                op = "jne";
                break;
        }
        out.print(indent + op);
        if (inst.getThenBB().postOrderNumber == inst.getCurrentBB().postOrderNumber + 1)
            out.println(" " + getLabel(inst.getElseBB()));
        else out.println(" " + getLabel(inst.getThenBB()));
    }

    @Override
    public void visit(Call inst) {
        printInstruction("call " + getLabel(inst.getCallee().getEntryBlock()));
    }

    @Override
    public void visit(Cmp inst) {
        out.print(indent + "cmp ");
        inst.getSrc1().accept(this);
        out.print(", ");
        inst.getSrc2().accept(this);
        out.println();
    }

    @Override
    public void visit(Jump inst) {
        out.println(indent + "jmp " + getLabel(inst.getTargetBB()));
    }

    @Override
    public void visit(Load inst) {
        out.print(indent + "mov ");
        inst.getDst().accept(this);
        out.print(", ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Move inst) {
        out.print(indent + "mov ");
        inst.getDst().accept(this);
        out.print(", ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Return inst) {
        printInstruction("ret");
    }

    @Override
    public void visit(Store inst) {
        out.print(indent + "mov ");
        inst.getDst().accept(this);
        out.print(", ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Unary inst) {
        String op = null;
        switch (inst.getOp()) {
            case NOT:
                op = "not";
                break;
            case NEG:
                op = "neg";
                break;
            case INC:
                op = "inc";
                break;
            case DEC:
                op = "dec";
                break;
            case POS:
            case NOTL:
                throw new RuntimeException();
        }
        out.print(indent + op + " ");
        inst.getDst().accept(this);
        out.print(" ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Phi inst) {

    }

    @Override
    public void visit(Lea inst) {
        out.print(indent + "lea ");
        inst.getDst().accept(this);
        out.print(", ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Push inst) {
        out.print(indent + "push ");
        inst.getSrc().accept(this);
        out.println();
    }

    @Override
    public void visit(Pop inst) {
        printInstruction("pop");
    }

    @Override
    public void visit(Storage storage) {
        if (storage instanceof Register) out.print(getName(storage));
        else if (storage instanceof Memory) {
            out.print("qword [");
            if (((Memory) storage).getBase() != null) {
                visit(((Memory) storage).getBase());
                if (((Memory) storage).getIndex() != null) out.print("+");
                else out.print(((Memory) storage).getOffset().getImmediate() > 0 ? "+" : "");
            }
            if (((Memory) storage).getIndex() != null) {
                visit(((Memory) storage).getIndex());
                out.print("*");
                visit(((Memory) storage).getScale());
                out.print(((Memory) storage).getOffset().getImmediate() > 0 ? "+" : "");
            }
            if (((Memory) storage).getOffset().getImmediate() != 0)
                visit(((Memory) storage).getOffset());
            out.print("]");
        }
    }

    @Override
    public void visit(Immediate immediate) {
        out.print(immediate.getImmediate());
    }

    private String createName(Storage storage, String name) {
        int cnt = nameCountMap.getOrDefault(name, 0) + 1;
        nameCountMap.put(name, cnt);
        String newName = name + "_" + cnt;
        storageStringMap.put(storage, newName);
        return newName;
    }

    public String getName(Storage storage) {
        if (storage instanceof VirtualRegister) {
            if (!(storage instanceof GlobalVariable)) return ((VirtualRegister) storage).color.getName();
            else {
                String name = storageStringMap.get(storage);
                name = name != null ? name : createName(storage, storage.getName() == null ? "t" : storage.getName());
                return name;
            }
        } else if (storage instanceof PhysicalRegister) return storage.getName();
        else return "NMDWSM";
    }

    private String createLabel(BasicBlock basicBlock, String name) {
        int cnt = nameCountMap.getOrDefault(name, 0) + 1;
        nameCountMap.put(name, cnt);
        String newName = name + "_" + cnt;
        basicBlockStringMap.put(basicBlock, newName);
        return newName;
    }

    private String getLabel(BasicBlock basicBlock) {
        if (basicBlock == null) return "";
        String name = basicBlockStringMap.get(basicBlock);
        return name != null ? name : createLabel(basicBlock, basicBlock.getName());
    }
}
