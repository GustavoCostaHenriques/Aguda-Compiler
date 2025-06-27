package aguda.codegen;

import java.util.Deque;
import java.util.List;
import java.util.Map;

import aguda.ast.*;
import aguda.codegen.datastructures.*;

public class GenerateCondExpr {

    private static CodeGenerator codeGenerator;
    private GenerateExpr generateExpr; // Remover 'static' e inicializar via construtor

    public GenerateCondExpr(CodeGenerator codeGenerator, GenerateExpr generateExpr) { // Adicionar GenerateExpr como parâmetro
        GenerateCondExpr.codeGenerator = codeGenerator;
        this.generateExpr = generateExpr; // Atribuir a instância passada
    }


    /**
     * Generates all the information for a conditional expression
     * 
     * @param ctx The context with the right scope for this expression
     * @param label The label where this expression exists
     * @param tt The label if the expression avaliates to true
     * @param ff The label if the expression avaliates to false
     * @param node The node that it is being executed
     * @return A variable with ReturnExpr type having the code, value, 
     *         type and label (exit label) of the expression
     */
    public ReturnCondExpr generateCondExpr(Deque<Map<String, Ctx>> ctx, String label, String tt, String ff, AstNode node) {
        /* --- Simple booleans --- */
        if (node instanceof BoolLiteral) {
            boolean val = ((BoolLiteral) node).value;
            String code = val ? "\tbr label %" + tt + "\n" : "\tbr label %" + ff + "\n";
            String outLabel = val ? tt : ff;
            return new ReturnCondExpr(code, outLabel);
        }

        /* --- Unary expressions --- */
        else if(node instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) node;

            if(unaryOp.op.equals("!")) {
                // Invertion of true and false labels
                return generateCondExpr(ctx, label, ff, tt, unaryOp.expr);
            } else {
                codeGenerator.addErrorMessage(node);
            }
        }

        /* --- Binary expressions --- */
        else if (node instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) node;
            
            if (binaryOp.op.equals("&&")) {
                String l2 = codeGenerator.getNextLabel("and");

                // e1 == true -> continues to e2 (l2)
                // e1 == false -> jumps to ff ff
                ReturnCondExpr r1 = generateCondExpr(ctx, label, l2, ff, binaryOp.left);

                // Uses the exit label of r1 to continue (case e1 == true)
                ReturnCondExpr r2 = generateCondExpr(ctx, l2, tt, ff, binaryOp.right);

                String r1Code = r1.getCode() != null ? r1.getCode() : "";
                String r2Code = r2.getCode() != null ? r2.getCode() : "";

                String code = r1Code + l2 + ":\n\t" + r2Code;

                return new ReturnCondExpr(code, r2.getLabel());

            } else if (binaryOp.op.equals("||")) {
                String l2 = codeGenerator.getNextLabel("or");

                // e1 == true -> jumps to tt
                // e1 == false -> continues to e2 (l2)
                ReturnCondExpr r1 = generateCondExpr(ctx, label, tt, l2, binaryOp.left);

                // Uses the exit label of ri to continue (case e1 == false)
                ReturnCondExpr r2 = generateCondExpr(ctx, l2, tt, ff, binaryOp.right);

                String r1Code = r1.getCode() != null ? r1.getCode() : "";
                String r2Code = r2.getCode() != null ? r2.getCode() : "";

                String code = r1Code + l2 + ":\n\t" + r2Code;

                return new ReturnCondExpr(code, r2.getLabel());
            } else if (List.of("==", "!=", "<", "<=", ">", ">=").contains(binaryOp.op)) {
                ReturnExpr left = generateExpr.generateExpr(ctx, label, binaryOp.left);
                ReturnExpr right = generateExpr.generateExpr(ctx, left.getLabel(), binaryOp.right);

                String cmpOp = null;

                switch (binaryOp.op) {
                    case "==": cmpOp = "eq"; break;
                    case "!=": cmpOp = "ne"; break;
                    case "<":  cmpOp = "slt"; break;
                    case "<=": cmpOp = "sle"; break;
                    case ">":  cmpOp = "sgt"; break;
                    case ">=": cmpOp = "sge"; break;
                    default: codeGenerator.addErrorMessage(node);
                }

                String llvmType = codeGenerator.convertAgudaTypetoLLVM(left.getType(), node);  
                String tmpVar = codeGenerator.getNextRegister();

                String code = (left.getCode() != null ? left.getCode() : "") + 
                            (right.getCode() != null ? right.getCode() : "") +
                            String.format("\t%s = icmp %s %s %s, %s\n", tmpVar, cmpOp, llvmType, left.getValue(), right.getValue()) +
                            String.format("\tbr i1 %s, label %%%s, label %%%s\n", tmpVar, tt, ff);

                return new ReturnCondExpr(code, right.getLabel());
            } else {
                codeGenerator.addErrorMessage(node);
            }
        }

        /* --- List of expressions --- */
        else if (node instanceof Expressions) {
            Expressions expressions = (Expressions) node;
            List<AstNode> exprs = expressions.expressions;
            StringBuilder code = new StringBuilder(); 

            for(int i = 0; i < exprs.size(); i++) {
                ReturnCondExpr returnCondExpr = generateCondExpr(ctx, label, tt, ff, exprs.get(i));
                label = returnCondExpr.getLabel();

                String codeExpr =  returnCondExpr.getCode();
                
                if(i == exprs.size() - 1) {
                    if(codeExpr != null) code.append(codeExpr);
                } else {
                    if(codeExpr != null) code.append(codeExpr).append(";");
                }
            }

            return new ReturnCondExpr(code.toString(), label);
        }

        /* --- Parenthical expressions --- */
        else if (node instanceof ParenthicalExpression) {
            ParenthicalExpression paren = (ParenthicalExpression) node;
            return generateCondExpr(ctx, label, tt, ff, paren.expression);
        }

        else {
            ReturnExpr returnExpr = generateExpr.generateExpr(ctx, label, node);
    
            String llvmCode = returnExpr.getCode() != null ? returnExpr.getCode() : "";
            String condVar = returnExpr.getValue(); 

            llvmCode += String.format("\n\tbr i1 %s, label %%%s, label %%%s\n", condVar, tt, ff);

            return new ReturnCondExpr(llvmCode, returnExpr.getLabel());
        }

        codeGenerator.addErrorMessage(node);
        return new ReturnCondExpr("", label);
    }
    
}
