package aguda.checker;

import aguda.ast.*;
import aguda.types.*;
import aguda.context.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.math.BigInteger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Checker {

    private final Context context;
    private final String currentFile;
    private boolean hasErrors = false;
    private final StringBuilder errorMessages = new StringBuilder();
    private int maxErrors;
    private int totalErrors;

    public Checker(String filePath, int maxErrors) {
        this.context = new Context(); // initial simbol table 
        this.currentFile = filePath;
        this.maxErrors = maxErrors;
    }

    public String getErrorMessage() {
        return errorMessages.toString();
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void registerInternalError(String message) {
        hasErrors = true;
        errorMessages.append(message).append("\n");
    }

    public Type typeof(AstNode node, boolean isDeclaration) {
        if (node instanceof Program program) {
            // 1. predeclaration of functions and global variables
            for (AstNode decl : program.declarations) {
                predeclare(decl);
            }

            // 2. check the types of the declarations
            for (AstNode decl : program.declarations) {
                typeof(decl, true); 
            }

            if (hasErrors) {
                errorMessages.append("Program has " + totalErrors + " semantic error(s), " + maxErrors + " or less, were shown above has requested.");
                throw new RuntimeException("");
            }
            return new UnitTypeChecker();
        } else if (node instanceof VariableDeclaration varDecl) {
            return checkVariableDeclaration(varDecl);
        } else if (node instanceof FunctionTypeDeclaration funDecl) {
            return checkFunctionDeclaration(funDecl);
        } else if (node instanceof Expressions exprs) {
            return checkExpressions(exprs);
        } else if (node instanceof LetExpression letExpr) {
            return checkLetExpression(letExpr);
        } else if (node instanceof IfExpression ifExpr) {
            return checkIfExpression(ifExpr);
        } else if (node instanceof SetExpression setExpr) {
            return checkSetExpression(setExpr);
        } else if (node instanceof WhileExpression whileExpr) {
            return checkWhileExpression(whileExpr);
        }else if (node instanceof CallFunctionExpression call) {
            return checkCallFunction(call);
        }else if (node instanceof BinaryOp binop) {
            return checkBinaryOp(binop);
        } else if (node instanceof ArrayAccess arrayAccess) {
            return checkArrayAccess(arrayAccess);
        } else if (node instanceof ArrayCreation arrayCreation) {
            return checkArrayCreation(arrayCreation);
        } else if (node instanceof ParenthicalExpression paren) {
            return typeof(paren.expression, false);
        } else if (node instanceof IntLiteral) {
            return new IntTypeChecker();
        } else if (node instanceof BoolLiteral) {
            return new BoolTypeChecker();
        } else if (node instanceof StringLiteral) {
            return new StringTypeChecker();
        } else if (node instanceof NullLiteral) {
            return new UnitTypeChecker(); 
        } else if (node instanceof Identifier id) {
            Type type = context.get(id.value);
            if (type == null) {
                error(id, "Undeclared variable '" + id.value + "'");
                return new UndeclaredTypeChecker(); // default type for undeclared variables
            } 
            if (id.value.equals("_") && !isDeclaration) {
                error(id, "Wildcard '_' cannot be used in expressions");
                return new UndeclaredTypeChecker();
            }
            return type;
        }
        else if (node instanceof UnaryOp unary) {
            Type operandType = typeof(unary.expr, false);

            switch (unary.op) {
                case "!" -> {
                    if (!(operandType instanceof BoolTypeChecker)) {
                        error(unary, "Unary '!' expects Bool operand, found: " + operandType);
                    }
                    return new BoolTypeChecker();
                }
                case "-" -> {
                    if (!(operandType instanceof IntTypeChecker)) {
                        error(unary, "Unary '-' expects Int operand, found: " + operandType);
                    }

                    if (unary.expr instanceof IntLiteral intLit) {
                        BigInteger value = intLit.toBigInteger();
                        BigInteger negated = value.negate();

                        BigInteger intMin = BigInteger.valueOf(Integer.MIN_VALUE);
                        BigInteger intMax = BigInteger.valueOf(Integer.MAX_VALUE);

                        if (negated.compareTo(intMin) < 0 || negated.compareTo(intMax) > 0) {
                            error(unary, "Result of unary minus is out of Int bounds: " + negated);
                        }
                    }
                    return new IntTypeChecker();
                }
                default -> {
                    error(unary, "Unknown unary operator: " + unary.op);
                    return new UnitTypeChecker(); // fallback
                }
            }
        }


        // TODO: other cases (BinaryExpr, Let, etc.)
        throw new RuntimeException("Unknown expression type: " + node.getClass());
    }

    private void predeclare(AstNode decl) {
        if (decl instanceof VariableDeclaration varDecl) {
            if (varDecl.id.equals("print") || varDecl.id.equals("length")) {
                error(varDecl, "Cannot use reserved name '" + varDecl.id + "' as a variable");
            }
            else {
                if (context.contains(varDecl.id) && !varDecl.id.equals("_")) {
                    error(varDecl, "Variable '" + varDecl.id + "' is already defined");
                } else {
                    Type type = convertType(varDecl.typeElem);
                    context.add(varDecl.id, type);
                }
            }
        } else if (decl instanceof FunctionTypeDeclaration funDecl && funDecl.idList instanceof IdList idList) {
            if (idList.id.equals("print") || idList.id.equals("length")) {
                error(funDecl, "Cannot use reserved name '" + idList.id + "' as a function");
            }
            else {
                if (context.contains(idList.id) && !idList.id.equals("_")) {
                    error(funDecl, "Function '" + idList.id + "' is already defined");
                } else {
                    Type type = convertType(funDecl.functionType);
                    context.add(idList.id, type);
                }
            }
        }
    }

    /****************************************************************************************
     *                                                                                      *
     *                        PROGRAM AND TOP LEVEL DECLARATIONS                            *
     *                                                                                      *
     ****************************************************************************************/

    private Type checkVariableDeclaration(VariableDeclaration decl) {
        // Note: the variable is already in the context, so we don't need to add it again
        Type declaredType = convertType(decl.typeElem);


        context.remove(decl.id);

        context.beginScope();

        Type actualType = typeof(decl.exprs, false);

        context.endScope();

        context.add(decl.id, declaredType);
        
        if (!declaredType.equals(actualType)) {
            AstNode lastExpr = null;
            if (decl.exprs instanceof Expressions expressions && !expressions.expressions.isEmpty()) {
                lastExpr = expressions.expressions.get(expressions.expressions.size() - 1);
            } else {
                lastExpr = decl.exprs;
            }

            error(lastExpr, "Declared type " + declaredType + " does not match actual type " + actualType);
        }
        return new UnitTypeChecker();
    }

    private Type checkFunctionDeclaration(FunctionTypeDeclaration decl) {
        // Note: the function is already in the context, so we don't need to add it again
        Type declaredType = convertType(decl.functionType);

        if (decl.idList instanceof IdList idList) {

            // Opens new scope for the parameters
            context.beginScope();

            // Obtain the parameters types
            if (declaredType instanceof FunctionTypeChecker fn) {
                Type from = fn.getFrom();
                if (from instanceof MultiTypeList paramTypes) {
                    List<Type> types = paramTypes.getTypes();
                    List<AstNode> ids = idList.ids;

                    if (types.size() != ids.size()) {
                        error(decl, "Function parameters count doesn't match type signature");
                    }

                    for (int i = 0; i < ids.size(); i++) {
                        AstNode param = ids.get(i);
                        if (param instanceof Identifier paramId) {
                            if (paramId.value.equals("print") || paramId.value.equals("length")) {
                                error(paramId, "Cannot use reserved name '" + paramId.value + "' as a parameter");
                            }
                            else {
                                context.add(paramId.value, types.get(i));
                            }
                        } 
                    }
                } else {
                    // Only one parameter
                    if (idList.ids.size() != 1) {
                        error(decl, "Function declared with multiple identifiers but type signature expects one");
                    }
                    AstNode param = idList.ids.get(0);
                    if (param instanceof Identifier paramId) {
                        if (paramId.value.equals("print") || paramId.value.equals("length")) {
                            error(paramId, "Cannot use reserved name '" + paramId.value + "' as a parameter");
                        }
                        else {
                            context.add(paramId.value, from);
                        }
                    }
                }
            }
        }

        // Now that the parameters are in the context, we can check the body of the function
        Type actualType = typeof(decl.exprs, false); 

        context.endScope(); // close the scope of the parameters

        if (declaredType instanceof FunctionTypeChecker fnType) {
            Type expectedReturnType = fnType.getTo();

            if (!expectedReturnType.equals(actualType)) {
                AstNode lastExpr = null;
                if (decl.exprs instanceof Expressions expressions && !expressions.expressions.isEmpty()) {
                    lastExpr = expressions.expressions.get(expressions.expressions.size() - 1);
                } else {
                    lastExpr = decl.exprs;
                }
                error(lastExpr, "Function body does not match declared return type " + expectedReturnType +
                                ". Found: " + actualType);
            }
        }

        return new UnitTypeChecker();
    }


    /****************************************************************************************
     *                                                                                      *
     *                                    EXPRESSIONS                                       *
     *                                                                                      *
     ****************************************************************************************/

    private Type checkExpressions(Expressions exprs) {
        List<AstNode> list = exprs.expressions;
        if (list.isEmpty()) return new UnitTypeChecker();

        for (int i = 0; i < list.size() - 1; i++) {
            typeof(list.get(i), false); // ensures semantics
        }
        return typeof(list.get(list.size() - 1), false);
    }

    private Type checkLetExpression(LetExpression letExpr) {
        Type declaredType = convertType(letExpr.type);

        context.beginScope();

        Type result = typeof(letExpr.blockExpr, false);

        context.endScope();

        if(!declaredType.equals(result)) {
            AstNode lastExpr = null;
            if (letExpr.blockExpr instanceof Expressions expressions && !expressions.expressions.isEmpty()) {
                lastExpr = expressions.expressions.get(expressions.expressions.size() - 1);
            } else {
                lastExpr = letExpr.blockExpr;
            }
            error(lastExpr, "Declared type " + declaredType + " does not match expression type " + result);
        }
        context.add(letExpr.id, declaredType);

        return new UnitTypeChecker(); // let returns Unit
    }

    private Type checkIfExpression(IfExpression ifExpr) {

        context.beginScope();

        Type condType = typeof(ifExpr.condition, false);

        context.endScope();

        if (!(condType instanceof BoolTypeChecker)) {
            error(ifExpr.condition, "Expected condition to be of type Bool, found " + condType);
        }

        context.beginScope();

        Type thenType = typeof(ifExpr.thenBranch, false);

        context.endScope();

        Type elseType = new UnitTypeChecker();

        if (ifExpr.elseBranch != null) {
            context.beginScope();
            elseType = typeof(ifExpr.elseBranch, false); // validates, but doesn't matter the tipo  of the body
            context.endScope();
        }

        if (!thenType.equals(elseType)) {
            error(ifExpr, "Expected both branches to return same type, found " + thenType + " and " + elseType);
        }

        return thenType;
    }

    private Type checkSetExpression(SetExpression setExpr) {
        Type lhsType = typeof(setExpr.lhs, false);
        Type rhsType = typeof(setExpr.value, false);

        if (!lhsType.equals(rhsType)) {
            error(setExpr, "Type mismatch in assignment: expected " + lhsType + ", found " + rhsType);
        }

        return new UnitTypeChecker(); // set returns Unit
    }

    private Type checkWhileExpression(WhileExpression whileExpr) {
        Type condType = typeof(whileExpr.condition, false);

        if (!(condType instanceof BoolTypeChecker)) {
            error(whileExpr.condition, "Condition of while must be Bool, found " + condType);
        }

        context.beginScope();
        typeof(whileExpr.body, false); // validates, but doesn't matter the tipo  of the body
        context.endScope();


        return new UnitTypeChecker();
    }

    private Type checkCallFunction(CallFunctionExpression call) {

        if (call.id.equals("print")) {
            List<AstNode> args = ((Expressions) call.exprs).expressions;
            if (args.size() != 1) {
                error(call, "'print' expects exactly 1 argument, found " + args.size());
                return new UnitTypeChecker();
            }
            typeof(args.get(0), false); // validar tipo do argumento, mesmo que seja qualquer tipo
            return new UnitTypeChecker();
        }

        if (call.id.equals("length")) {
            List<AstNode> args = ((Expressions) call.exprs).expressions;
            if (args.size() != 1) {
                error(call, "'length' expects exactly 1 argument, found " + args.size());
                return new IntTypeChecker(); // retornar algo para seguir a execução
            }

            Type argType = typeof(args.get(0), false);
            if (!(argType instanceof ArrayTypeChecker arrayType)) {
                error(args.get(0), "'length' expects an array argument, found " + argType);
                return new IntTypeChecker();
            }

            return new IntTypeChecker();
        }
        Type type = context.get(call.id);
        boolean hasErrors = false;

        if (type == null) {
            error(call, "Undeclared function '" + call.id + "'");
            hasErrors = true;
        }

        if (!(type instanceof FunctionTypeChecker)) {
            error(call, "Identifier '" + call.id + "' is not a function");
            hasErrors = true;
        }

        if (hasErrors) {
            return new UnitTypeChecker();
        }

        FunctionTypeChecker fn = (FunctionTypeChecker) type;
        // Check arguments
        List<AstNode> args = ((Expressions) call.exprs).expressions;

        if (fn.getFrom() instanceof MultiTypeList params) {
            List<Type> expectedTypes = params.getTypes();

            if (expectedTypes.size() != args.size()) {
                error(call, "Function '" + call.id + "' expects " + expectedTypes.size()
                    + " arguments, got " + args.size());
            }

            for (int i = 0; i < args.size(); i++) {
                Type actualArg = typeof(args.get(i), false);
                Type expectedArg = expectedTypes.get(i);

                if (!actualArg.equals(expectedArg)) {
                    error(args.get(i), "Argument " + (i + 1) + " of '" + call.id
                        + "' expects " + expectedArg + ", found " + actualArg);
                }
            }
        } else {
            // unary function
            if (args.size() != 1) {
                error(call, "Function '" + call.id + "' expects 1 argument, got " + args.size());
            }

            Type actualArg = typeof(args.get(0), false);
            if (!fn.getFrom().equals(actualArg)) {
                error(args.get(0), "Function '" + call.id
                    + "' expects argument of type " + fn.getFrom() + ", found " + actualArg);
            }
        }

        return fn.getTo();
    }

    private Type checkBinaryOp(BinaryOp binOp) {
        Type leftType = typeof(binOp.left, false);
        Type rightType = typeof(binOp.right, false);

        switch (binOp.op) {
            case "+", "-", "*", "/", "%", "^" -> {
                if (!(leftType instanceof IntTypeChecker) || !(rightType instanceof IntTypeChecker)) {
                    error(binOp, "Arithmetic operations require Int operands, found " + leftType + " and " + rightType);
                }
                return new IntTypeChecker();
            }

            case "<", "<=", ">", ">=" -> {
                if (!(leftType instanceof IntTypeChecker) || !(rightType instanceof IntTypeChecker)) {
                    error(binOp, "Relational operators '" + binOp.op + "' require Int operands, found " + leftType + " and " + rightType);
                }
                return new BoolTypeChecker();
            }

            case "==", "!=" -> {
                if (!leftType.equals(rightType)) {
                    error(binOp, "Equality operator '" + binOp.op + "' requires operands of the same type, found " + leftType + " and " + rightType);
                }
                return new BoolTypeChecker();
            }

            case "&&", "||" -> {
                if (!(leftType instanceof BoolTypeChecker) || !(rightType instanceof BoolTypeChecker)) {
                    error(binOp, "Logical operations require Bool operands, found " + leftType + " and " + rightType);
                }
                return new BoolTypeChecker();
            }

            default -> throw new RuntimeException("Unknown binary operator: " + binOp.op);
        }
    }

    private Type checkArrayAccess(ArrayAccess arrayAccess) {
        for (AstNode index : arrayAccess.indices) {
            Type indexType = typeof(index, false);
            if (!(indexType instanceof IntTypeChecker)) {
                error(index, "Array index must be of type Int, found " + indexType);
            }
        }

        Type arrayType = typeof(arrayAccess.array, false);

        if (!(arrayType instanceof ArrayTypeChecker arr)) {
            error(arrayAccess, "Trying to access something that is not an array: " + arrayType);
            return new UndeclaredTypeChecker();
        }

        int remainingDimensions = arr.getDimensions() - arrayAccess.indices.size();
        if (remainingDimensions < 0) {
            error(arrayAccess, "Too many indices for array of dimension " + arr.getDimensions());
            return new UndeclaredTypeChecker();
        }

        if (remainingDimensions == 0) {
            return arr.getElementType(); // final element type, like Int
        } else {
            return new ArrayTypeChecker(arr.getElementType(), remainingDimensions);
        }
    }


    private Type checkArrayCreation(ArrayCreation array) {
        // Determines the base type
        Type baseType = switch (array.baseType) {
            case "Int" -> new IntTypeChecker();
            case "Bool" -> new BoolTypeChecker();
            case "String" -> new StringTypeChecker();
            case "Unit" -> new UnitTypeChecker();
            default -> throw new RuntimeException("Unknown array base type: " + array.baseType);
        };

        int numberOfDimensions = array.getDimensions().size();
        for (ArrayCreation.Dimension dim : array.getDimensions()) {
            if(dim.sizeExpr == null) {
                continue;
            }
            Type sizeType = typeof(dim.sizeExpr, false);
            if (!(sizeType instanceof IntTypeChecker)) {
                error(dim.sizeExpr, "Array size must be Int, found: " + sizeType);
            }

            boolean isarray = false;

            Type initType = typeof(dim.initExpr, false);
            if(initType instanceof ArrayTypeChecker arr) {
                isarray = true;
                Type elemType = arr.getElementType(); // Get the element type of the array
                if (!elemType.equals(baseType)) {
                    error(dim.initExpr, "Arrayy initialization type mismatch: expected " + baseType + ", found " + elemType);
                }
            }
            if (!initType.equals(baseType) && !isarray) {
                error(dim.initExpr, "Array initialization type mismatch: expected " + baseType + ", found " + initType);
            }
        }

        return new ArrayTypeChecker(baseType, numberOfDimensions);
    }

    private Type convertType(AstNode node) {
        if (node instanceof BasicType basic) {
            return switch (basic.basicType) {
                case "Int" -> new IntTypeChecker();
                case "Bool" -> new BoolTypeChecker();
                case "String" -> new StringTypeChecker();
                case "Unit" -> new UnitTypeChecker();
                default -> throw new RuntimeException("Unknown basic type: " + basic.basicType);
            };
        }

        if (node instanceof ArrayType arr) {
            int dimensions = arr.dimensions;
            Type idType = convertType(arr.basicType);
            return new ArrayTypeChecker(idType, dimensions); 
        }

        if (node instanceof TypeList typeList) {
            List<Type> convertedTypes = new ArrayList<>();
            for (AstNode t : typeList.typesParam) {
                convertedTypes.add(convertType(t));
            }
            return new MultiTypeList(convertedTypes); 
        }

        if (node instanceof FunctionType fn) {
            Type from = convertType(fn.typeList);
            Type to = convertType(fn.returnType);
            return new FunctionTypeChecker(from, to);
        }

        throw new RuntimeException("Unknown AST type: " + node.getClass());
    }


    private void error(AstNode node, String message) {
        if(totalErrors < maxErrors) {
            hasErrors = true;
            int line = node.getLine();
            int column = node.getColumn();
            String location = String.format("Test Failed ❌\nError: (%d, %d) %s", line, column, message);

            errorMessages.append(location).append("\n");

            try {
                List<String> lines = Files.readAllLines(Paths.get(currentFile));
                if (line > 0 && line <= lines.size()) {
                    String codeLine = lines.get(line - 1);
                    errorMessages.append("↳  ").append(codeLine).append("\n");
                    errorMessages.append("   ").append(" ".repeat(column)).append("^\n");
                }
            } catch (IOException e) {
                errorMessages.append("   Could not read source file for context.\n");
            }
        }
        totalErrors ++;
    }
}
