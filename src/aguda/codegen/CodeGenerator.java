package aguda.codegen;

import aguda.ast.*;
import aguda.types.*;
import aguda.checker.*;
import aguda.types.*;
import aguda.codegen.datastructures.*;

import java.util.List;
import java.util.HashMap; // To map AGUDA variables to LLVM values/pointers
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;


public class CodeGenerator {

    private StringBuilder llvmCode;
    public StringBuilder helperFunctions;
    private StringBuilder errorMessage;
    public int tempCounter; // Counter for generating unique LLVM temporary register names
    private int labelCounter; // Counter for generating unique LLVM label names
    private Deque<Map<String, Ctx>> ctx = new ArrayDeque<>();
    private boolean powFunctionDefined = false;
    private int maxErrors;
    private Set<String> reportedErrorLocations = new HashSet<>();

    private static GenerateExpr generateExpr;
    private static GenerateCondExpr generateCondExpr;
    
    /* --- Ctx operations --- */
    private Map<String, Ctx> currentCtx() {
        return ctx.peek();
    }

    public void pushCtx() {
        ctx.push(new HashMap<>());
    }

    public void popCtx() {
        ctx.pop();
    }

    public void addToCtx(String name, Ctx value) {
        currentCtx().put(name, value);
    }

    public Ctx lookupCtx(String name) {
        for (Map<String, Ctx> ctxNew : ctx) {
            if (ctxNew.containsKey(name)) {
                return ctxNew.get(name);
            }
        }
        return null;
    }
    /* ----------------------- */

    public CodeGenerator(int maxErrors) {
        this.llvmCode = new StringBuilder();
        this.helperFunctions = new StringBuilder();
        this.errorMessage = new StringBuilder();;
        this.tempCounter = 1;
        this.labelCounter = 1;
        this.ctx.push(new HashMap<>());
        this.maxErrors = maxErrors;


        generateExpr = new GenerateExpr(this);
        generateCondExpr = new GenerateCondExpr(this, generateExpr);

        generateExpr.setGenerateCondExpr(generateCondExpr);    
    }

    /**
     * Generates LLVM IR code from the given Abstract Syntax Tree.
     * Assumes the AST has been successfully validated by the Checker
     *
     * @param ast The root node of the validated AST (must be a Program node)
     * @return A string containing the generated LLVM IR code
     */
    public String generate(AstNode ast) {
        if (!(ast instanceof Program)) {
            // This should not happen if Main.java is working correctly
            addErrorMessage(ast);
        }

        // Initial context
        buildInitialContext(ast);

        // Generate code for top-level declarations (functions and supported global variables)
        Program program = (Program) ast;
        for (AstNode declaration : program.declarations) {
            if(declaration instanceof FunctionTypeDeclaration) {
                AstNode idList = ((FunctionTypeDeclaration) declaration).idList;
                String id = null;
                if(idList instanceof IdList) {
                    id = ((IdList) idList).id;
                }

                AstNode exprs = ((FunctionTypeDeclaration) declaration).exprs;

                Ctx functionCtx = lookupCtx(id);
                String reg = functionCtx.getReg();
                Type type = functionCtx.getType();
                String typeStr = convertAgudaTypetoLLVM(type, ast);  

                llvmCode.append("\ndefine ").append(typeStr).append(" ").append(reg).append("(");

                List<AstNode> ids = null;
                List<AstNode> types = null;
                if(idList instanceof IdList) {
                    ids = ((IdList) idList).ids;
                }

                if(((FunctionTypeDeclaration) declaration).functionType instanceof FunctionType) {
                    FunctionType functionTypeNode = (FunctionType) ((FunctionTypeDeclaration) declaration).functionType;
                    if(functionTypeNode.typeList instanceof TypeList) {
                        types = ((TypeList) functionTypeNode.typeList).typesParam;
                    } else if(functionTypeNode.typeList instanceof BasicType) {
                        types = new ArrayList<>();
                        types.add(functionTypeNode.typeList);
                    }
                }

                pushCtx();

                addArgumentsToCtx(ids, types, id, ast);

                llvmCode.append(") {\n").append("entry:\n");

                int localWildCounter = 0;

                for (AstNode paramIdNode : ids) {
                    if (paramIdNode instanceof Identifier) {
                        String paramName = ((Identifier) paramIdNode).value;
                        String ctxLookupName = paramName.equals("_") ? "__wild" + localWildCounter++ : paramName;
                        Ctx paramCtx = lookupCtx(ctxLookupName);
                        String llvmType = convertAgudaTypetoLLVM((paramCtx != null ? paramCtx.getType() : null), ast);
                        String regId = paramCtx != null ? paramCtx.getReg() : null; 
                        String ptrName = paramCtx != null ? paramCtx.getPtr() : null;

                        llvmCode.append("\t").append(ptrName).append(" = alloca ").append(llvmType).append("\n");
                        llvmCode.append("\tstore ").append(llvmType).append(" ").append(regId).append(", ").append(llvmType).append("* ").append(ptrName).append("\n");
                    }
                }

                ReturnExpr body = generateExpr.generateExpr(ctx, "entry", exprs);

                if (body.getCode() != null && !body.getCode().isBlank()) {
                    String[] instructions = body.getCode().split(";");
                    for (String instr : instructions) {
                        instr = instr.trim();
                        if (!instr.isEmpty()) {
                            llvmCode.append("\t").append(instr).append("\n");
                        }
                    }
                }

                if(id.equals("main")) {
                    llvmCode.append("\tret i1 1\n}\n");
                } else {
                    llvmCode.append("\tret ").append(typeStr).append(" ").append((body.getValue() != null && !body.getValue().equals(""))? body.getValue() : "1").append("\n");
                    llvmCode.append("}\n");
                }

                popCtx();

            } else if (declaration instanceof VariableDeclaration) {
                String id = ((VariableDeclaration) declaration).id;
                AstNode expr = ((VariableDeclaration) declaration).exprs;

                Ctx variableCtx = lookupCtx(id);
                String reg = variableCtx.getReg();
                Type type = variableCtx.getType();
                ReturnExpr returnExpr = generateExpr.generateExpr(ctx, null, expr); 
                String value = returnExpr.getValue();
                String typeStr = convertAgudaTypetoLLVM(type, ast);  

                if (returnExpr.getCode() != null) { // This happens only in the case of unary operations
                    if(expr instanceof Expressions exprs) {
                        if(exprs.expressions.size() == 1 && exprs.expressions.get(0) instanceof UnaryOp unaryOp) {
                            if(unaryOp.expr instanceof IntLiteral intLiteral) { // Negation integer case
                                value = "-" + String.valueOf(intLiteral.value);
                            } else if(unaryOp.expr instanceof BoolLiteral boolLiteral) { // Negation boolean case
                                value = boolLiteral.value ? "false" : "true";
                            } 
                        }
                    }
                }

                llvmCode.append(reg).append(" = global ").append(typeStr).append(" ").append(value).append("\n"); 
            }
        }

        return helperFunctions.toString() + llvmCode.toString();
    }

    /* ---------------------- HELPER FUNCTIONS ------------------- */

    /**
     * Adds all the global variables and functions to the context
     * 
     * @param ast The root node of the validated AST
     */
    private void buildInitialContext(AstNode ast) {
        Program program = (Program) ast;
        for (AstNode declaration : program.declarations) {
            if (declaration instanceof FunctionTypeDeclaration) {
                //Adds the name of the function to the context
                String id = null;
                AstNode idList = ((FunctionTypeDeclaration) declaration).idList;
                if (idList instanceof IdList) {
                    id = ((IdList) idList).id;
                }
                Type functionType = convertAstToType(((FunctionTypeDeclaration) declaration).functionType);
                Ctx functionCtx = new Ctx(functionType, "@" + id.replaceAll("[^a-zA-Z0-9_]", "_"), null, true, null);
                addToCtx(id, functionCtx);
            } else if (declaration instanceof VariableDeclaration) {
                // Adds the name of the variable to the context
                String id = ((VariableDeclaration) declaration).id;
                Type variableType = convertAstToType(((VariableDeclaration) declaration).typeElem);
                Ctx variableCtx = new Ctx(variableType, "@" + id, "%ptr_" + id.replaceAll("[^a-zA-Z0-9_]", "_") + tempCounter++, false, null);
                addToCtx(id, variableCtx);
            } 
        }
    }

    /**
     * Converts an AST node to a type
     *
     * @param astNode The node that is going to be converted to type
     * @return Type of said node
     */
    public Type convertAstToType(AstNode astNode) {
        if (astNode instanceof BasicType basic) {
            Type type = switch (basic.basicType) {
                case "Int" -> new IntTypeChecker();
                case "Bool" -> new BoolTypeChecker();
                case "Unit" -> new UnitTypeChecker();
                default -> null;
            };

            if (type == null) {
                addErrorMessage(astNode);
            }

            return type;

        } else if (astNode instanceof FunctionType functionType) {
            AstNode returnType = functionType.returnType;

            if (returnType instanceof BasicType basicReturn) {
                Type type = switch (basicReturn.basicType) {
                    case "Int" -> new IntTypeChecker();
                    case "Bool" -> new BoolTypeChecker();
                    case "Unit" -> new UnitTypeChecker();
                    default -> null;
                };

                if (type == null) {
                    addErrorMessage(astNode);
                    return null;
                }

                return type;

            } else {
                addErrorMessage(astNode);
                return null;
            }

        } else {
            addErrorMessage(astNode);
            return null;
        }
    }


    /**
     * Converts an aguda type to an LLVM type
     * 
     * @param type The aguda type
     * @param node The node where this expression resides
     * @returns The LLVM type
     */
    public String convertAgudaTypetoLLVM(Type type, AstNode node) {
        if (type instanceof IntTypeChecker) {
            return "i32";
        } else if (type instanceof BoolTypeChecker) {
            return "i1";
        } else if (type instanceof UnitTypeChecker) {
            return "i1";
        } else {
            addErrorMessage(node);
            return null;
        }
    }

    /**
     * Converts the aguda binary operator to a LLVM operation
     * 
     * @param op The aguda binary operator
     * @return A string with the new LLVM operator
     */
    public String convertOperatorToLLVM(String op) {
        return switch(op) {
            case "+" -> "add";
            case "-" -> "sub";
            case "*" -> "mul";
            case "/" -> "sdiv";
            case "%" -> "srem";
            case "<" -> "icmp slt";
            case "<=" -> "icmp sle";
            case ">" -> "icmp sgt";
            case ">=" -> "icmp sge";
            case "==" -> "icmp eq";
            case "!=" -> "icmp ne";
            case "&&" -> "and";
            case "||" -> "or";
            default -> "exponent";
        };
    }

    /**
     * Adds the arguments of a function to the Ctx
     *
     * @param ids The list of ids to add
     * @param types The respective type of each argument
     * @param functionName The name of the function of this arguments
     * @param node The nome where the arguments are
     */
    private void addArgumentsToCtx(List<AstNode> ids, List<AstNode> types, String functionName, AstNode node) {
        if (types == null) {
            addErrorMessage(node);
            return;
        }

        int wildcardCounter = 0;

        for (int i = 0; i < ids.size(); i++) {
            if (types.get(i) == null) {
                addErrorMessage(node);
                continue;
            }

            if (ids.get(i) instanceof Identifier) {
                String id = ((Identifier) ids.get(i)).value;

                Type type = convertAstToType(types.get(i));
                String typeStr = convertAgudaTypetoLLVM(type, node);

                // Handle wildcards
                boolean isWildcard = id.equals("_");
                if (isWildcard) {
                    id = "__wild" + wildcardCounter++; // generate unique internal name
                }
                // Add the argument to the context
                Ctx argumentCtx = new Ctx(
                    type,
                    "%" + id,
                    "%ptr_" + id.replaceAll("[^a-zA-Z0-9_]", "_") + tempCounter++,
                    false,
                    functionName
                );
                addToCtx(id, argumentCtx);

                // Append the argument to the global LLVM code stringbuilder
                llvmCode.append(typeStr).append(" ").append("%").append(id);

                // Add ", " if not the last argument
                if (i != ids.size() - 1) {
                    llvmCode.append(", ");
                }
            }
        }
    }

    /**
     * Generates code for the exponent operation
     */
    public void definePowFunction() {
        if (powFunctionDefined) return;

        helperFunctions.append("""
        define i32 @powi(i32 %base, i32 %exp) {
        entry:
            %result = alloca i32
            store i32 1, i32* %result
            %i = alloca i32
            store i32 0, i32* %i

            br label %loop

        loop:
            %i_val = load i32, i32* %i
            %cond = icmp slt i32 %i_val, %exp
            br i1 %cond, label %body, label %end

        body:
            %res_val = load i32, i32* %result
            %mul = mul i32 %res_val, %base
            store i32 %mul, i32* %result

            %i_val2 = load i32, i32* %i
            %inc = add i32 %i_val2, 1
            store i32 %inc, i32* %i

            br label %loop

        end:
            %final = load i32, i32* %result
            ret i32 %final
        }\n
        """);

        powFunctionDefined = true;
    }

    /**
     * Prints some declaration of a variable to the helperfunctions stringbuilder (used for print calls)
     * 
     * @param content The content that will be printed
     * @param type The type  of the content (Int, Bool or Unit)
     * @param name The name of the new variable defined
     */
    public void definePrintfFormat(String content, String type, String name) {
        if (!helperFunctions.toString().contains(name)) {
            helperFunctions.append(name)
                .append(" = private unnamed_addr constant ")
                .append(type).append(" c\"").append(content).append("\", align 1\n");
        }
    }

    /**
     * Adds a message to the error message global variable
     * 
     * @param node The node where the error happened
     */
    public void addErrorMessage(AstNode node) {
        if (maxErrors > 0) {
            String locationKey = "";
            String locationDisplay = "";
            if (node != null && node.getLine() != -1 && node.getColumn() != -1) {
                locationKey = String.format("%d:%d", node.getLine(), node.getColumn());
                locationDisplay = String.format(" (%d,%d)", node.getLine(), node.getColumn());
            }

            if (!locationKey.isEmpty() && reportedErrorLocations != null && reportedErrorLocations.contains(locationKey)) {
                return; 
            }

            if (!locationKey.isEmpty()) {
                reportedErrorLocations.add(locationKey);
            }

            String expressionString = "";
            if (node != null) {
                String fullPrint = node.print(0); // Assume que print(0) retorna a representação do nó
                // Pega apenas a primeira linha
                int newLineIndex = fullPrint.indexOf('\n');
                if (newLineIndex != -1) {
                    expressionString = fullPrint.substring(0, newLineIndex);
                } else {
                    expressionString = fullPrint;
                }
            }

            errorMessage.append("Not implemented: ").append("Generation code for").append(locationDisplay).append(" expression '").append(expressionString).append("'\n");            
            maxErrors--;
        }
    }


    /**
     * Creates an unique register
     * 
     * @return A string with the unique register
     */
    public String getNextRegister() {
        return "%v" + tempCounter++;
    }

    /**
     * Creates an unique label
     * 
     * @param Name The name of the label
     * @return A string with the unique label ( %nameLabelCounter (e.g. %true1) )
     */
    public String getNextLabel(String name) {
        return name + labelCounter++;
    }

    /**
     * Gets the error message
     * 
     * @return The string of the error message global variable
     */
    public String getErrorMessage() {
        return errorMessage.toString();
    }
}