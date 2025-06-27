package aguda.codegen;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import aguda.ast.*;
import aguda.types.*;
import aguda.codegen.datastructures.Ctx;
import aguda.codegen.datastructures.ReturnCondExpr;
import aguda.codegen.datastructures.ReturnExpr;




public class GenerateExpr {

    private static CodeGenerator codeGenerator;
    private GenerateCondExpr condExpresion;

    public GenerateExpr(CodeGenerator codeGenerator) {
        GenerateExpr.codeGenerator = codeGenerator;

    }

    public void setGenerateCondExpr(GenerateCondExpr condExpr) {
        this.condExpresion = condExpr;
    }
    
    /**
     * Generates all the information for an expression
     * 
     * @param ctx The context with the right scope for this expression
     * @param label The label where this expression exists
     * @param node The node that it is being executed
     * @return A variable with ReturnExpr type having the code, value, 
     *         type and label (exit label) of the expression
     */
    public ReturnExpr generateExpr(Deque<Map<String, Ctx>> ctx, String label, AstNode node) {
        /* --- Literal expressions --- */
        if (node instanceof IntLiteral) {
            ReturnExpr returnExprInt = new ReturnExpr(null, ((IntLiteral) node).value, new IntTypeChecker(), label);
            return returnExprInt;
        } else if (node instanceof BoolLiteral) {
            String value = ((BoolLiteral) node).value ? "true" : "false";
            ReturnExpr returnExpr = new ReturnExpr(null, value, new BoolTypeChecker(), label);
            return returnExpr;
        } else if (node instanceof NullLiteral) {
            ReturnExpr returnExpr = new ReturnExpr(null, "1", new UnitTypeChecker(), label);
            return returnExpr;
        } else if (node instanceof StringLiteral) {
            codeGenerator.addErrorMessage(node);
        } else if (node instanceof ArrayType) {
            codeGenerator.addErrorMessage(node);
        } else if (node instanceof Identifier) {
            Identifier id = (Identifier) node;

            Ctx var = codeGenerator.lookupCtx(id.value);
            String llvmType = codeGenerator.convertAgudaTypetoLLVM((var != null ? var.getType() : null), node);
            
            if(var != null) {
                String reg = codeGenerator.getNextRegister();
                Type type = var.getType() != null ? var.getType() : null;

                if(type != null && type.equals(new UnitTypeChecker())){
                    return new ReturnExpr("", var.getReg(), type, label);
                }

                String code = "";
                if (var.getPtr() != null && var.getReg() != null && !var.getReg().startsWith("@")) {
                    // Se tens um ponteiro, fazes load
                    code = String.format("\t%s = load %s, %s* %s\n", reg, llvmType, llvmType, var.getPtr());
                    return new ReturnExpr(code, reg, type, label);
                } else if (var.getReg() != null && var.getReg().startsWith("@")) {
                    // Global variable
                    code = String.format("\t%s = load %s, %s* %s\n", reg, llvmType, llvmType, var.getReg());
                    return new ReturnExpr(code, reg, type, label);
                }
            }
            
            return new ReturnExpr("", var != null ? var.getReg() : null, var != null ? var.getType() : null, label);
        }

        /* --- Unary expressions --- */
        else if (node instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) node;
            ReturnExpr returnExpr = generateExpr(ctx, label, unaryOp.expr);

            // If the operation is "- expr"
            if (unaryOp.op.equals("-")) {
                String type = codeGenerator.convertAgudaTypetoLLVM(returnExpr.getType(), node);
                String result = codeGenerator.getNextRegister();
                StringBuilder code = new StringBuilder();
                if(returnExpr.getCode() != null) code.append(returnExpr.getCode()).append(";");
                code.append(result).append(" = sub ").append(type).append(" 0, ").append(returnExpr.getValue());
                return new ReturnExpr(code.toString(), result, returnExpr.getType(), returnExpr.getLabel());
            }

            // If the operation is "! expr"
            else if (unaryOp.op.equals("!")) {
                int negations = 0;
                AstNode current = unaryOp;
                // Conta o número de negações
                while (current instanceof UnaryOp && ((UnaryOp) current).op.equals("!")) {
                    negations++;
                    current = ((UnaryOp) current).expr;
                }

                ReturnExpr innerExpr = generateExpr(ctx, label, current);

                if (negations % 2 == 1) {
                    String typeInner = codeGenerator.convertAgudaTypetoLLVM(innerExpr.getType(), node);
                    String resultInner = codeGenerator.getNextRegister();
                    StringBuilder codeInner = new StringBuilder();
                    if(innerExpr.getCode() != null) codeInner.append(innerExpr.getCode()).append(";\n");
                    codeInner.append(resultInner).append(" = xor ").append(typeInner).append(" 1, ").append(innerExpr.getValue());
                    return new ReturnExpr(codeInner.toString(), resultInner, innerExpr.getType(), innerExpr.getLabel());
                } else {
                    return innerExpr;
                }
            }
        }

        /* --- Binary expressions --- */
        if (node instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) node;
            String opAguda = binaryOp.op;

            if (opAguda.equals("&&") || opAguda.equals("||")) {
                String tt = codeGenerator.getNextLabel("true");
                String ff = codeGenerator.getNextLabel("false");
                String join = codeGenerator.getNextLabel("join");
                StringBuilder code = new StringBuilder();

                ReturnCondExpr condExpr = condExpresion.generateCondExpr(ctx, label, tt, ff, binaryOp);

                if (condExpr.getCode() != null) code.append(condExpr.getCode());
                code.append("\n").append(tt).append(":\n");
                code.append("\tbr label %").append(join).append("\n");
                code.append("\n").append(ff).append(":\n");
                code.append("\tbr label %").append(join).append("\n");
                code.append("\n").append(join).append(":\n\t");

                String resultReg = codeGenerator.getNextRegister();
                code.append(resultReg).append(" = phi i1 [ true, %").append(tt).append(" ], [ false, %").append(ff).append(" ]\n");

                return new ReturnExpr(code.toString(), resultReg, new BoolTypeChecker(), join);
            }

            ReturnExpr left = generateExpr(ctx, label, binaryOp.left);
            ReturnExpr right = generateExpr(ctx, label, binaryOp.right);

            StringBuilder code = new StringBuilder();

            if (left.getCode() != null) code.append(left.getCode()).append(";");
            if (right.getCode() != null) code.append(right.getCode()).append(";");

            Type leftType = left.getType(); 
            String llvmType = codeGenerator.convertAgudaTypetoLLVM(leftType, node);

            String result;

            if (opAguda.equals("^")) {
                codeGenerator.definePowFunction();

                String res = codeGenerator.getNextRegister();
                code.append(res).append(" = call ").append(llvmType)
                    .append(" @powi(").append(llvmType).append(" ")
                    .append(left.getValue()).append(", ")
                    .append(llvmType).append(" ").append(right.getValue()).append(")\n");
                result = res;

            } else {
                String llvmOp = codeGenerator.convertOperatorToLLVM(opAguda);
                result = codeGenerator.getNextRegister();
                code.append(result).append(" = ").append(llvmOp).append(" ")
                    .append(llvmType).append(" ")
                    .append(left.getValue()).append(", ")
                    .append(right.getValue()).append("\n");
            }

            Type returnType = null;

            switch (opAguda) {
                case "+" -> returnType = new IntTypeChecker();
                case "-" -> returnType = new IntTypeChecker();
                case "*" -> returnType = new IntTypeChecker();
                case "/" -> returnType = new IntTypeChecker();
                case "%" -> returnType = new IntTypeChecker();
                case "^" -> returnType = new IntTypeChecker();
                case "<" -> returnType = new BoolTypeChecker();
                case "<=" -> returnType = new BoolTypeChecker();
                case ">" -> returnType = new BoolTypeChecker();
                case ">=" -> returnType = new BoolTypeChecker();
                case "==" -> returnType = new BoolTypeChecker();
                case "!=" -> returnType = new BoolTypeChecker();
                case "&&" -> returnType = new BoolTypeChecker();
                case "||" -> returnType = new BoolTypeChecker(); 
                default -> codeGenerator.addErrorMessage(node);
            }
                
            return new ReturnExpr(code.toString(), result, returnType, label);
        }
        
        /* --- List of expressions --- */
        else if (node instanceof Expressions) {
            Expressions expressions = (Expressions) node;
            List<AstNode> exprs = expressions.expressions;
            StringBuilder code = new StringBuilder(); 
            String value = null;
            Type type = null;

            for(int i = 0; i < exprs.size(); i++) {
                ReturnExpr returnExpr = generateExpr(ctx, label, exprs.get(i));
                label = returnExpr.getLabel();

                String codeExpr =  returnExpr.getCode();
                
                if(i == exprs.size() - 1) {
                    if(codeExpr != null) code.append(codeExpr);
                    value = returnExpr.getValue();
                    type = returnExpr.getType();
                } else {
                    if(codeExpr != null) code.append(codeExpr).append(";");
                }
            }

            return new ReturnExpr(code.toString(), value, type, label);
        } 

        /* --- Let expressions --- */
        else if (node instanceof LetExpression) {
            LetExpression letExpr = (LetExpression) node;

            
            codeGenerator.pushCtx();
            ReturnExpr valueExpr = generateExpr(ctx, label, letExpr.blockExpr);
            label = valueExpr.getLabel();
            codeGenerator.popCtx(); 

            Type declaredAgudaType = codeGenerator.convertAstToType(letExpr.type); 
            String llvmType = codeGenerator.convertAgudaTypetoLLVM(declaredAgudaType, node); 

            String regAlloca = "%ptr_" + letExpr.id.replaceAll("[^a-zA-Z0-9_]", "_") + codeGenerator.tempCounter++;
            String llvmName = "%" + letExpr.id.replaceAll("[^a-zA-Z0-9_]", "_") + codeGenerator.tempCounter++;

            StringBuilder code = new StringBuilder();
            if (valueExpr.getCode() != null) code.append(valueExpr.getCode());

            String valueToStore = valueExpr.getValue() != null ? valueExpr.getValue() : "1";
            Type actualValueType = valueExpr.getType() != null ? valueExpr.getType() : null; // Actual type of the expression's result

            // Check if the actual value type matches the declared type.
            // If not, perform a "cast" or conversion for Unit type.
            if ( actualValueType != null && !actualValueType.equals(declaredAgudaType)) {
                if (declaredAgudaType instanceof UnitTypeChecker) {
                    if (actualValueType instanceof IntTypeChecker) { // e.g., int -> unit
                        valueToStore = "1"; // Canonical value for Unit (i1 true)
                    } else if (actualValueType instanceof BoolTypeChecker) { // e.g., bool -> unit
                        valueToStore = "1"; // Canonical value for Unit (i1 true)
                    } else {
                        codeGenerator.addErrorMessage(node);
                    }
                } else {
                    codeGenerator.addErrorMessage(node);
                }
            }


            code.append(String.format("\t%s = alloca %s\n", regAlloca, llvmType));
            code.append(String.format("\tstore %s %s, %s* %s\n", llvmType, valueToStore, llvmType, regAlloca));

            // Add to the context *after* the popCtx(), so it's in the correct outer scope
            Ctx variableCtx = new Ctx(declaredAgudaType, llvmName, regAlloca, false, null);
            codeGenerator.addToCtx(letExpr.id, variableCtx);

            code.append(String.format("\t%s = load %s, %s* %s\n", llvmName, llvmType, llvmType, regAlloca));

            return new ReturnExpr(code.toString(), llvmName, declaredAgudaType, label);
        }

        /* --- Set expressions --- */
        else if (node instanceof SetExpression) {
            SetExpression setExpr = (SetExpression) node;

            // Generate the new value
            codeGenerator.pushCtx();
            ReturnExpr valueExpr = generateExpr(ctx, label, setExpr.value);
            codeGenerator.popCtx();
            label = valueExpr.getLabel();

            // Find the register of the variable to update
            String id = null;
            if (setExpr.lhs instanceof Identifier) {
                id = ((Identifier) setExpr.lhs).value;
            }
            Ctx variableCtx = codeGenerator.lookupCtx(id);
            String ptr = (variableCtx != null ? variableCtx.getPtr() : null);
            String llvmType = codeGenerator.convertAgudaTypetoLLVM((variableCtx != null ? variableCtx.getType() : null), node);
            
            // Generate the code
            StringBuilder code = new StringBuilder();
            if (valueExpr.getCode() != null) code.append(valueExpr.getCode());
            code.append(String.format("\n\tstore %s %s, %s* %s\n", llvmType, valueExpr.getValue(), llvmType, ptr ));

            return new ReturnExpr(code.toString(), null, new UnitTypeChecker(), label);
        }

        /* --- Parenthical expressions --- */
        else if (node instanceof ParenthicalExpression) {
            ParenthicalExpression paren = (ParenthicalExpression) node;
            codeGenerator.pushCtx();
            ReturnExpr returnExpr = generateExpr(ctx, label, paren.expression);
            codeGenerator.popCtx();
            
            return returnExpr;
        }

        /* --- Call function expressions --- */
        else if (node instanceof CallFunctionExpression) {
            CallFunctionExpression call = (CallFunctionExpression) node;
            String funcName = call.id;
            AstNode exprsNode = call.exprs;

            List<ReturnExpr> argExprs = new ArrayList<>();
            List<String> llvmArgs = new ArrayList<>();
            StringBuilder code = new StringBuilder();

            if(funcName.equals("print")) { // Special case for print

                if (call.exprs != null) {
                    if (!(call.exprs instanceof Expressions)) {
                        argExprs.add(generateExpr(ctx, label, call.exprs));
                    } else {
                        for (AstNode argNode : ((Expressions) call.exprs).expressions) {
                            argExprs.add(generateExpr(ctx, label, argNode));
                        }
                    }
                }

                for (ReturnExpr arg : argExprs) {
                    if (arg.getCode() != null) code.append(arg.getCode()).append(";");

                    Type argType = arg.getType() != null ? arg.getType() : null;

                    if (argType instanceof IntTypeChecker) {
                        String fmtGlobal = "@.fmt.int";
                        codeGenerator.definePrintfFormat("%d\\00", "[3 x i8]", fmtGlobal); // without \n

                        String gep = codeGenerator.getNextRegister();
                        code.append(gep).append(" = getelementptr inbounds [3 x i8], [3 x i8]* ")
                            .append(fmtGlobal).append(", i32 0, i32 0;");
                        code.append("call i32 (i8*, ...) @printf(i8* ")
                            .append(gep).append(", i32 ").append(arg.getValue()).append(")");
                    }

                    else if (argType instanceof BoolTypeChecker) {
                        // %cmp = icmp eq i1 %bool, 1
                        // %res = select i1 %cmp, i8* trueStr, i8* falseStr
                        // call printf(res)

                        codeGenerator.definePrintfFormat("true\\00", "[5 x i8]", "@.str.true");
                        codeGenerator.definePrintfFormat("false\\00", "[6 x i8]", "@.str.false");

                        String boolVal = arg.getValue();
                        String cmp = codeGenerator.getNextRegister();
                        String res = codeGenerator.getNextRegister();

                        code.append(cmp).append(" = icmp eq i1 ").append(boolVal).append(", 1;");
                        code.append(res).append(" = select i1 ").append(cmp)
                            .append(", i8* getelementptr ([5 x i8], [5 x i8]* @.str.true, i32 0, i32 0), ")
                            .append("i8* getelementptr ([6 x i8], [6 x i8]* @.str.false, i32 0, i32 0);");

                        code.append("call i32 (i8*, ...) @printf(i8* ").append(res).append(")");
                    }

                    else if (argType instanceof UnitTypeChecker) {
                        codeGenerator.definePrintfFormat("unit\\00", "[5 x i8]", "@.str.unit");
                        String gep = codeGenerator.getNextRegister();
                        code.append(gep).append(" = getelementptr inbounds [3 x i8], [3 x i8]* @.str.unit, i32 0, i32 0;");
                        code.append("call i32 (i8*, ...) @printf(i8* ").append(gep).append(")");
                    } 

                    else if (argType instanceof StringTypeChecker) {
                        codeGenerator.addErrorMessage(node);
                    }
                }

                if (!codeGenerator.helperFunctions.toString().contains("@printf")) {
                    codeGenerator.helperFunctions.append("\ndeclare i32 @printf(i8*, ...)\n\n");
                }

                return new ReturnExpr(code.toString(), "", new UnitTypeChecker(), label);

            }

            Type returnType = null;

            if (exprsNode != null) {
                if (!(exprsNode instanceof Expressions)) {
                    // Single argument (not wrapped in Expressions list)
                    ReturnExpr arg = generateExpr(ctx, label, exprsNode);
                    argExprs.add(arg);
                } else {
                    // Multiple arguments
                    Expressions exprs = (Expressions) exprsNode;
                    for (AstNode argNode : exprs.expressions) {
                        ReturnExpr arg = generateExpr(ctx, label, argNode);
                        argExprs.add(arg);
                    }
                }

                for (ReturnExpr arg : argExprs) {
                    if (arg.getCode() != null) code.append(arg.getCode()).append(";");
                    String argType = codeGenerator.convertAgudaTypetoLLVM(arg.getType(), node);
                    llvmArgs.add(argType + " " + arg.getValue());
                }
            }

            // Obtains the return type of the function (from the context)
            Ctx found = codeGenerator.lookupCtx(funcName);
            if (found != null) {
                returnType = found.getType();
            } else {
                // defensive fallback
                returnType = new UnitTypeChecker();
            }

            String llvmReturnType = codeGenerator.convertAgudaTypetoLLVM(returnType, node);
            String result = codeGenerator.getNextRegister();

            code.append(result).append(" = call ").append(llvmReturnType)
                .append(" @").append(funcName).append("(")
                .append(String.join(", ", llvmArgs)).append(")");

            return new ReturnExpr(code.toString(), result, returnType, label);
        }

        /* --- If expressions --- */
        else if (node instanceof IfExpression ie) {
            // 1. Generate labels
            String tt = codeGenerator.getNextLabel("true");
            String ff = codeGenerator.getNextLabel("false");
            String join = codeGenerator.getNextLabel("join");

            // 2. Generate code of the condition
            codeGenerator.pushCtx();
            ReturnCondExpr cond = condExpresion.generateCondExpr(ctx, label, tt, ff, ie.condition);
            codeGenerator.popCtx();


            // 3. Generate code for the Then branch
            codeGenerator.pushCtx();
            ReturnExpr thenExpr = generateExpr(ctx, tt, ie.thenBranch);
            codeGenerator.popCtx();
            Type thenType = thenExpr.getType();
            String thenTypeStr = codeGenerator.convertAgudaTypetoLLVM(thenType, node);
            String codeThen = thenExpr.getCode() != null ? thenExpr.getCode() + "\n\tbr label %" + join : "br label %" + join;


            // 4. Generate code for the Else branch
            codeGenerator.pushCtx();
            ReturnExpr elseExpr = generateExpr(ctx, ff, ie.elseBranch);
            codeGenerator.popCtx();
            String codeElse = elseExpr.getCode() != null ? elseExpr.getCode() + "\n\tbr label %" + join : "br label %" + join ;

            // 5. Generate code for the join
            String result = codeGenerator.getNextRegister();

            String thenValue = null;
            if (thenExpr.getValue() != null) {
                if (thenExpr.getValue().equals("") ) {
                    thenValue = "1";
                } else {
                    thenValue = thenExpr.getValue(); 
                }
            } else {
                thenValue = "1";
            }

            String elseValue = null;
            if (elseExpr.getValue() != null) {
                if (elseExpr.getValue().equals("") ) {
                    elseValue = "1";
                } else {
                    elseValue = elseExpr.getValue(); 
                }
            } else {
                elseValue = "1";
            }

            String phi = result + " = phi " + thenTypeStr + " [ " + thenValue + ", %" + (thenExpr.getLabel() == null ? tt : thenExpr.getLabel()) + " ], [ " + elseValue + ", %" + (elseExpr.getLabel() == null ? ff : elseExpr.getLabel()) + " ]";
            
            // 6. Concatenates the code
            String condCode = cond.getCode() != null ? cond.getCode() : "";

            String code = ""
                + condCode
                + "\n" + tt + ":\n\t"
                + codeThen
                + "\n" + ff + ":\n\t"
                + codeElse
                + "\n" + join + ":\n\t"
                + phi;
        
        
            return new ReturnExpr(code, result, thenType, join);
        
        } 

        /* --- While expressions --- */
        else if (node instanceof WhileExpression) {
            WhileExpression w = (WhileExpression) node;

            String condLabel = codeGenerator.getNextLabel("while_cond");
            String bodyLabel = codeGenerator.getNextLabel("while_body");
            String afterLabel = codeGenerator.getNextLabel("while_end");

            // 1. Jump to the condition
            String code = String.format("br label %%%s\n", condLabel);

            // 2. Generate code for the condition
            codeGenerator.pushCtx();
            ReturnCondExpr condExpr = condExpresion.generateCondExpr(ctx, condLabel, bodyLabel, afterLabel, w.condition);
            codeGenerator.popCtx();


            // 3. Generate code for the body
            codeGenerator.pushCtx();
            ReturnExpr bodyExpr = generateExpr(ctx, bodyLabel, w.body);
            codeGenerator.popCtx();

            // 4. Complete the code
            code += String.format("%s:\n%s", condLabel, condExpr.getCode());
            code += String.format("%s:\n%s", bodyLabel, bodyExpr.getCode());
            code += String.format("\tbr label %%%s\n", condLabel); // loop back
            code += String.format("%s:\n", afterLabel);

            return new ReturnExpr(code, null, new UnitTypeChecker(), afterLabel); // while expressions always return unit type 
        } else {
            return new ReturnExpr(null, null, null, null);
        }
    }
}