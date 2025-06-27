package aguda.ast;

import aguda.ast.*;
import aguda.parser.*;
import java.util.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.Token;

public class AstBuilder extends AgudaBaseVisitor<AstNode> {

    /****************************************************************************************
     *                                                                                      *
     *                        PROGRAM AND TOP LEVEL DECLARATIONS                            *
     *                                                                                      *
     ****************************************************************************************/

    @Override
    public AstNode visitProgram(AgudaParser.ProgramContext ctx) {
        List<AstNode> decls = new ArrayList<>();
        for (AgudaParser.DeclarationContext declCtx : ctx.declaration()) {
            decls.add(visit(declCtx));
        }
        return new Program(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), decls);
    }

    @Override
    public AstNode visitDeclaration(AgudaParser.DeclarationContext ctx) {
        if(ctx.variableDeclaration() != null) {
            return visit(ctx.variableDeclaration());
        }
        else {
            return visit(ctx.functionTypeDeclaration());
        }
    }

    @Override
    public AstNode visitFunctionTypeDeclaration(AgudaParser.FunctionTypeDeclarationContext ctx) {
        AstNode idList = visitIdList(ctx.idList(), ctx.ID().getText());
        AstNode type = visit(ctx.functionType());
        AstNode exprs = visit(ctx.exprs());

        return new FunctionTypeDeclaration(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), idList, type, exprs);
    }

    @Override
    public AstNode visitVariableDeclaration(AgudaParser.VariableDeclarationContext ctx) {
        String id = ctx.ID().getText();
        AstNode type = visit(ctx.typeElem());
        AstNode exprs = visit(ctx.exprs());

        return new VariableDeclaration(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), id, type, exprs);
    }

    /****************************************************************************************
     *                                                                                      *
     *                                    VARIABLES                                         *
     *                                                                                      *
     ****************************************************************************************/
    
    public AstNode visitIdList(AgudaParser.IdListContext ctx, String id) {
        List<AstNode> ids = new ArrayList<>();
        for (TerminalNode idToken : ctx.ID()) {
            Token token = idToken.getSymbol(); 
            ids.add(new Identifier(token.getLine(), token.getCharPositionInLine(), token.getText()));
        }
        return new IdList(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), id, ids);
    }


    /****************************************************************************************
     *                                                                                      *
     *                                      TYPES                                           *
     *                                                                                      *
     ****************************************************************************************/

    @Override
    public AstNode visitBasicType(AgudaParser.BasicTypeContext ctx) {
        if (ctx.INTTYPE() != null) {
            return new BasicType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "Int");
        } else if (ctx.BOOLTYPE() != null) {
            return new BasicType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "Bool");
        } else if (ctx.STRINGTYPE() != null) {
            return new BasicType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "String");
        } else if (ctx.UNITTYPE() != null) {
            return new BasicType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "Unit");
        } 
        return null;
    }

    @Override
    public AstNode visitArrayType(AgudaParser.ArrayTypeContext ctx) {
        AstNode basicType = visit(ctx.basicType());
        int dimensions = ctx.LEFTBRACKETS().size(); // uses the number of '[' to know how many dimensions exist
        return new ArrayType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), basicType, dimensions);
    }

    @Override
    public AstNode visitFunctionType(AgudaParser.FunctionTypeContext ctx) {
        if (ctx.LEFTPAREN() != null) {
            // (T1, T2, ...) -> Ret
            AstNode typeList = visit(ctx.typeList());
            AstNode returnType = visit(ctx.typeElem(0));
            return new FunctionType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), typeList, returnType);
        } else {
            // T1 -> T2
            AstNode from = visit(ctx.typeElem(0));
            AstNode to = visit(ctx.typeElem(1));
            return new FunctionType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), from, to);
        }
    }


    @Override
    public AstNode visitTypeElem(AgudaParser.TypeElemContext ctx) {
         if (ctx.basicType() != null) {
            return new BasicType(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.basicType().getText());
        } else if (ctx.arrayType() != null) {
            return visit(ctx.arrayType());
        }
        return null;
    }

    @Override
    public AstNode visitTypeList(AgudaParser.TypeListContext ctx) {
        List<AstNode> types = new ArrayList<>();
        for (AgudaParser.TypeElemContext typeCtx : ctx.typeElem()) {
            types.add(visit(typeCtx));
        }
        return new TypeList(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), types);
    }

    /****************************************************************************************
     *                                                                                      *
     *                                    EXPRESSIONS                                       *
     *                                                                                      *
     ****************************************************************************************/

    @Override
    public AstNode visitExprs(AgudaParser.ExprsContext ctx) {
        List<AstNode> exprs = new ArrayList<>();
        for (AgudaParser.ExprContext exprCtx : ctx.expr()) {
            exprs.add(visit(exprCtx));
        }
        return new Expressions(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), exprs, ";");
    }

    @Override
    public AstNode visitExprsList(AgudaParser.ExprsListContext ctx) {
        List<AstNode> exprs = new ArrayList<>();
        for (AgudaParser.BlockexpressionContext blockexpressionCtx : ctx.blockexpression()) {
            exprs.add(visit(blockexpressionCtx));
        }
        return new Expressions(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), exprs, ",");
    }

    @Override
    public AstNode visitExpr(AgudaParser.ExprContext ctx) {
        if (ctx.letexpressions() != null) {
            return visit(ctx.letexpressions());
        } else if (ctx.setexpression() != null) {
            return visit(ctx.setexpression());
        } else if (ctx.ifexpression() != null) {
            return visit(ctx.ifexpression());
        } else if (ctx.whileexpression() != null) {
            return visit(ctx.whileexpression());
        } else if (ctx.logicalexpression() != null) {
            return visit(ctx.logicalexpression());
        } else if (ctx.arraycreationexpression() != null) {
            return visit(ctx.arraycreationexpression());
        } 

        return null; // fallback in case of error
    }

               /********************************************************************
                *                                                                  *
                *              LOGICAL AND ARITHMETIC EXPRESSIONS                  *
                *                                                                  *
                ********************************************************************/

    @Override
    public AstNode visitLogicalexpression(AgudaParser.LogicalexpressionContext ctx) {
        if (ctx.op != null) {
            AstNode left = visit(ctx.logicalexpression());
            AstNode right = visit(ctx.logical());
            return new BinaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.op.getText(), left, right);
        } else {
            return visit(ctx.logical());
        }
    }

    @Override
    public AstNode visitLogical(AgudaParser.LogicalContext ctx) {
        if (ctx.op != null) {
            AstNode left = visit(ctx.logical());
            AstNode right = visit(ctx.unaryLogicalExpr());
            return new BinaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.op.getText(), left, right);
        }

        return visit(ctx.unaryLogicalExpr());
    }

    @Override
    public AstNode visitUnaryLogicalExpr(AgudaParser.UnaryLogicalExprContext ctx) {
        if(ctx.NOT() != null) {
            AstNode expr = visit(ctx.unaryLogicalExpr());
            return new UnaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "!", expr);
        }
        return visit(ctx.arithmeticExpr());
    }

    @Override
    public AstNode visitArithmeticExpr(AgudaParser.ArithmeticExprContext ctx) {
        if (ctx.op != null) {
            AstNode left = visit(ctx.arithmeticExpr());
            AstNode right = visit(ctx.term());
            return new BinaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.op.getText(), left, right);
        } else {
            return visit(ctx.term());
        }
    }

    @Override
    public AstNode visitTerm(AgudaParser.TermContext ctx) {
        if (ctx.op != null) {
            AstNode left = visit(ctx.term());
            AstNode right = visit(ctx.factor());
            return new BinaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.op.getText(), left, right);
        } else {
            return visit(ctx.factor());
        }
    }

    @Override
    public AstNode visitFactor(AgudaParser.FactorContext ctx) {
        AstNode base = visit(ctx.atom());
        if (ctx.POW() != null) {
            AstNode exponent = visit(ctx.factor());
            return new BinaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), "^", base, exponent);
        }
     
        return base;
    }

    @Override
    public AstNode visitAtom(AgudaParser.AtomContext ctx) {
        if(ctx.LEFTBRACKETS().isEmpty()) {
            return visit(ctx.primary());
        } else {
            // The first element is primary
            AstNode array = visit(ctx.primary());

            // Next comes the [blockexpression]
            List<AstNode> indices = ctx.blockexpression().stream()
                .map(this::visit)
                .toList();

            return new ArrayAccess(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), array, indices);
        }
    }

    @Override
    public AstNode visitPrimary(AgudaParser.PrimaryContext ctx) {
        if (ctx.exprsList() != null) {
            String id = ctx.ID().getText();
            AstNode exprs = visit(ctx.exprsList());
            return new CallFunctionExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), id, exprs);
        } else if (ctx.ID() != null) {
            return new Identifier(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.ID().getText());
        } else if (ctx.MINUS() != null) {
            return new UnaryOp(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.MINUS().getText(), visit(ctx.primary()));
        } else if (ctx.INT() != null) {
            return new IntLiteral(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.INT().getText());
        } else if (ctx.BOOLEAN() != null) {
            return new BoolLiteral(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.STRING() != null) {
            // Remove the quotes from the string literal
            String text = ctx.STRING().getText();
            return new StringLiteral(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), text);
        } else if (ctx.NULL() != null) {
            return new NullLiteral(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        } else if (ctx.blockexpression() != null) {
            AstNode blockExpr = visit(ctx.blockexpression());
            return new ParenthicalExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), blockExpr);
        } 
        return null;
    }

               /********************************************************************
               *                                                                  *
               *                         SET EXPRESSIONS                          *
               *                                                                  *
               ********************************************************************/

    @Override
    public AstNode visitSetexpression(AgudaParser.SetexpressionContext ctx) {
        AstNode lefthandside = visit(ctx.lefthandside());
        AstNode expr = visit(ctx.expr());

        return new SetExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), lefthandside, expr);
    }

    @Override
    public AstNode visitLefthandside(AgudaParser.LefthandsideContext ctx) {
        if (ctx.ID() != null) {
                AstNode base = new Identifier(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.ID().getText());
            
            if (ctx.blockexpression().isEmpty()) {
                return base;
            }

            List<AstNode> indices = new ArrayList<>();
            for (AgudaParser.BlockexpressionContext blockCtx : ctx.blockexpression()) {
                indices.add(visit(blockCtx));
            }

            return new ArrayAccess(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), base, indices);
        } else {
            // The first element is primary
            AstNode array = visit(ctx.primary());

            // Next comes the [blockexpression]
            List<AstNode> indices = ctx.blockexpression().stream()
                .map(this::visit)
                .toList();

            return new ArrayAccess(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), array, indices);        
        }
    }

               /********************************************************************
               *                                                                  *
               *                        IF EXPRESSIONS                            *
               *                                                                  *
               ********************************************************************/

    @Override
    public AstNode visitIfexpression(AgudaParser.IfexpressionContext ctx) {
        AstNode ifExpr = visit(ctx.expr(0));
        AstNode thenExpr = visit(ctx.expr(1));
        AstNode elseExpr = null;
        if (ctx.expr().size() > 2) {
            elseExpr = visit(ctx.expr(2));
        }
        return new IfExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ifExpr, thenExpr, elseExpr);
    }

               /********************************************************************
                *                                                                  *
                *                       WHILE EXPRESSIONS                          *
                *                                                                  *
                ********************************************************************/

    @Override
    public AstNode visitWhileexpression(AgudaParser.WhileexpressionContext ctx) {
        AstNode condition = visit(ctx.expr(0));
        AstNode body = visit(ctx.expr(1));
        return new WhileExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), condition, body);
    }

               /********************************************************************
                *                                                                  *
                *                    ARRAY CREATION EXPRESSIONS                    *
                *                                                                  *
                ********************************************************************/   

    @Override
    public AstNode visitArraycreationexpression(AgudaParser.ArraycreationexpressionContext ctx) {
        String baseType = ctx.basicType().getText();
        List<ArrayCreation.Dimension> dimensions = new ArrayList<>();

        int blockIndex = 0;
        for (int i = 0; i < ctx.LEFTBRACKETS().size(); i++) {
            if (ctx.PIPE(i) != null) {
                // There is an expression before and after the PIPE
                AstNode size = visit(ctx.blockexpression(blockIndex));
                AstNode init = visit(ctx.blockexpression(blockIndex + 1));
                dimensions.add(new ArrayCreation.Dimension(size, init));
                blockIndex += 2;
            } else {
                // Is a []
                dimensions.add(new ArrayCreation.Dimension(null, null));
            }
        }
        return new ArrayCreation(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), baseType, dimensions);
    }

               /********************************************************************
                *                                                                  *
                *                        LET EXPRESSIONS                           *
                *                                                                  *
                ********************************************************************/

    @Override
    public AstNode visitLetexpressions(AgudaParser.LetexpressionsContext ctx) {
        String id = ctx.ID().getText();
        AstNode type = visit(ctx.typeElem());
        AstNode expr = visit(ctx.expr());

        return new LetExpression(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), id, type, expr);
    }

               /********************************************************************
                *                                                                  *
                *                       BLOCK EXPRESSIONS                          *
                *                                                                  *
                ********************************************************************/

    @Override
    public AstNode visitBlockexpression(AgudaParser.BlockexpressionContext ctx) {
        List<AstNode> exprs = new ArrayList<>();
        for (AgudaParser.ExprContext exprCtx : ctx.expr()) {
            exprs.add(visit(exprCtx));
        }
        return new Expressions(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), exprs, ";");
    }   
}