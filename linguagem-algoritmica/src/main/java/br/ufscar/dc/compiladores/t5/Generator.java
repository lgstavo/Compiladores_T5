package br.ufscar.dc.compiladores.t5;

import static br.ufscar.dc.compiladores.t5.SemanticUtils.mapType;
import static br.ufscar.dc.compiladores.t5.SemanticUtils.verifyType;

import br.ufscar.dc.compiladores.t5.SymbolTable.EntryType;
import br.ufscar.dc.compiladores.t5.SymbolTable.TypeT5;

import java.util.stream.Stream;


public class Generator extends LinguagemAlgoritmicaBaseVisitor<Void>{

    StringBuilder output = new StringBuilder();

    SymbolTable table = new SymbolTable();
    Scopes scopes = new Scopes();
    static Scopes nestedScopes = new Scopes();

    public String convertType(TypeT5 auxTypeT5) {
        String returnType = null;
        if (auxTypeT5 != null) {
            switch (auxTypeT5) {
                case INTEGER:
                    returnType = "int";
                    break;
                case LITERAL:
                    returnType = "char";
                    break;
                case REAL:
                    returnType = "float";
                    break;
                default:
                    break;
            }
        }
        return returnType;
    }

    public TypeT5 convertTypeT5(String type){
        TypeT5 returnType = TypeT5.INVALID;

        switch (type) {
            case "literal":
                returnType = TypeT5.LITERAL;
                break;
            case "inteiro":
                returnType = TypeT5.INTEGER;
                break;
            case "real":
                returnType = TypeT5.REAL;
                break;
            case "logico":
                returnType = TypeT5.LOGIC;
                break;
            default:
                break;
        }

        return returnType;
    }

    public String verifyCType(String type){

        String returnType = null;

        switch (type) {
            case "inteiro":
                returnType = "int";
                break;
            case "literal":
                returnType = "char";
                break;
            case "real":
                returnType = "float";
                break;
            default:
                break;
        }


        return returnType;
    }

    public String verifyParamType(String type){
        String returnType = null;

        switch (type) {
            case "int":
                returnType = "d";
                break;
            case "float":
                returnType = "f";
                break;
            case "char":
                returnType = "s";
                break;
            default:
                break;
        }
        return returnType;
    }

    public String verifyParamTypeT5(TypeT5 auxTypeT5){

        String returnType = null;

        if (auxTypeT5 != null) {
            switch (auxTypeT5) {
                case INTEGER:
                    returnType = "d";
                    break;
                case REAL:
                    returnType = "f";
                    break;
                case LITERAL:
                    returnType = "s";
                    break;
                default:
                    break;
            }
        }
        return returnType;
    }

    public boolean verifyTableType(SymbolTable table, String type){
        return table.exists(type);
    }

    public String getCaseLimits(String str, boolean isLeft){
        String strAux;

        if(str.contains(".")){
            int cont = 0;
            boolean proceed = true;

            while(proceed){
                strAux = str.substring(cont);

                if(strAux.startsWith("."))
                    proceed = false;
                else
                    cont++;
            }

            if(isLeft)
                strAux = str.substring(0, cont);
            else
                strAux = str.substring(cont + 2);
        }
        else
            strAux = str;

        return strAux;
    }

    public String breakArg(String total, int valor){
        String argAux;
        boolean proceed = true;
        int cont = 0;

        total = total.substring(1);

        while (proceed){
            argAux = total.substring(cont);

            if(argAux.startsWith("=") || argAux.startsWith("<>"))
                proceed=false;
            else
                cont++;
        }

        if(valor==0){
            argAux = total.substring(0, cont);
        }
        else {
            total = total.substring(cont+1);
            cont=0;
            proceed=true;
            while(proceed){
                argAux = total.substring(cont);
                if(argAux.startsWith(")"))
                    proceed=false;
                else
                    cont++;
            }
            argAux=total.substring(0, cont);
        }

        return argAux;
    }

    public String breakExp(String total, int valor){
        String argAux;
        boolean proceed = true;
        int cont = 0;

        while (proceed){
            argAux = total.substring(cont);
            if(argAux.startsWith("+") || argAux.startsWith("-") || argAux.startsWith("*") || argAux.startsWith("/"))
                proceed=false;
            else
                cont++;

        }

        if(valor==0)
            argAux = total.substring(0, cont);
        else
            argAux = total.substring(cont+1);

        return argAux;
    }


    public String verificaOp(String total){
        String returnOP = null;

        if (total.contains("+"))
            returnOP = "+";
        else if (total.contains("-"))
            returnOP = "-";
        else if (total.contains("*"))
            returnOP = "*";
        else if (total.contains("/"))
            returnOP = "/";

        return returnOP;

    }
    @Override
    public Void visitPrograma(LinguagemAlgoritmicaParser.ProgramaContext ctx){
        
        output.append("#include <stdio.h>\n");
        output.append("#include <stdlib.h>\n");
        output.append("\n");

        visitDeclaracoes(ctx.declaracoes());

        output.append("\nint main() {\n");

        visitCorpo(ctx.corpo());

        output.append("\nreturn 0;\n");
        output.append("}\n");

        return null;
    }
    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx){
        String str;

        // Verifica se é uma declaração de constante.
        if (ctx.valor_constante() != null) {
            str = "#define " + ctx.IDENT().getText() + " " + ctx.valor_constante().getText() + "\n";
            output.append(str);
            // Verifica se é a criação de um registro.  
        } else if (ctx.tipo() != null) {
            SymbolTable escopoAtual = scopes.getCurrentScope();
            scopes.createNewScope();

            output.append("typedef struct {\n");
            super.visitRegistro(ctx.tipo().registro());
            scopes.leaveScope();

            escopoAtual.add(ctx.IDENT().getText(), TypeT5.REGISTER, EntryType.VARIABLE);

            str = "} " + ctx.IDENT().getText() + ";\n";
            output.append(str);
            // Caso em que é uma declaração de um tipo básico.
        } else if (ctx.variavel() != null)
            visitVariavel(ctx.variavel());

        return null;
    }
    @Override
    public Void visitVariavel(LinguagemAlgoritmicaParser.VariavelContext ctx) {

        SymbolTable actualScope = scopes.getCurrentScope();
        boolean extendedType = false;
        String str;

        if(ctx.tipo().tipo_estendido()!= null){
            String varName;
            String variableType = ctx.tipo().getText();
            TypeT5 auxTypeT5;
            boolean isPointer = false;

            if(variableType.contains("^")){
                isPointer = true;
                variableType = variableType.substring(1);
            }

            if(verifyTableType(actualScope, variableType)){
                extendedType = true;
                variableType = variableType.substring(1);
            }

            if(verifyTableType(actualScope, variableType)){
                extendedType = true;
                auxTypeT5 = TypeT5.EXTENDEDTYPE;
            }else{
                auxTypeT5 = convertTypeT5(variableType);
                variableType = convertType(auxTypeT5);
            }

            if(isPointer==true){
                variableType += "*";
            }

            for (LinguagemAlgoritmicaParser.IdentificadorContext ictx : ctx.identificador()) {
                varName = ictx.getText();

                if (extendedType)
                    actualScope.add(varName, TypeT5.REGISTER, EntryType.VARIABLE);
                else
                    actualScope.add(varName, auxTypeT5, EntryType.VARIABLE);

                // Impressão das declarações do programa.
                if (auxTypeT5 == TypeT5.LITERAL) {
                    str = variableType + " " + varName + "[80];\n";
                    output.append(str);
                } else {
                    str = variableType + " " + varName + ";\n";
                    output.append(str);
                }
            }
        }else{
            scopes.createNewScope();

            output.append("struct{\n");
            for(LinguagemAlgoritmicaParser.VariavelContext vctx : ctx.tipo().registro().variavel())
                visitVariavel(vctx);
            str = "}" + ctx.identificador(0).getText() + ";\n";
            output.append(str);

            scopes.leaveScope();
            actualScope.add(ctx.identificador(0).getText(), TypeT5.REGISTER, EntryType.VARIABLE);
        }
        return null;
    }
    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx){

        String str;

        SymbolTable actualScope = scopes.getCurrentScope();
        scopes.createNewScope();

        SymbolTable paramScope = scopes.getCurrentScope();
        String type, varNames;

        if(ctx.tipo_estendido() != null)
            output.append(verifyCType(ctx.tipo_estendido().getText()));
        else
            output.append("void");

        str = " " + ctx.IDENT().getText() + "(";
        output.append(str);

        if(ctx.parametros() != null){
            for(LinguagemAlgoritmicaParser.ParametroContext pctx : ctx.parametros().parametro()){
                type = verifyCType(pctx.tipo_estendido().getText());
                varNames = "";

                for(LinguagemAlgoritmicaParser.IdentificadorContext ictx : pctx.identificador()){
                    varNames += ictx.getText();
                    paramScope.add(ictx.getText(), convertTypeT5(pctx.tipo_estendido().getText()), EntryType.VARIABLE);
                }

                if(type.equals("char")){
                    type = "char*";
                }

                str = type + " " + varNames;
                output.append(str);
            }
        }
        output.append(") {\n");

        if(ctx.tipo_estendido() != null){
            actualScope.add(ctx.IDENT().getText(), convertTypeT5(ctx.tipo_estendido().getText()), EntryType.FUNCTION);
        }
        else
            actualScope.add(ctx.IDENT().getText(), TypeT5.VOID, EntryType.PROCEEDING);

        for(LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmd()){
            visitCmd(cctx);
        }

        output.append("}\n");
        scopes.leaveScope();

        return null;
    }
    @Override
    public Void visitParcela_nao_unario(LinguagemAlgoritmicaParser.Parcela_nao_unarioContext ctx){

        if (ctx.identificador() != null)
            output.append(ctx.identificador().getText());

        super.visitParcela_nao_unario(ctx);

        return null;
    }
    @Override
    public Void visitParcela_unario (LinguagemAlgoritmicaParser.Parcela_unarioContext ctx) {
        if (!ctx.expressao().get(0).getText().contains("(")) {
            output.append(ctx.getText());
        } else {
            output.append("(");
            super.visitParcela_unario(ctx);
            output.append(")");
        }

        return null;
    }

    @Override
    public Void visitOp_relacional(LinguagemAlgoritmicaParser.Op_relacionalContext ctx) {

        String strRetorno = ctx.getText();

        if (ctx.getText().contains("="))
            if (!ctx.getText().contains("<=") || !ctx.getText().contains(">="))
                strRetorno = "==";

        output.append(strRetorno);

        super.visitOp_relacional(ctx);

        return null;
    }

    @Override
    public Void visitCmdRetorne(LinguagemAlgoritmicaParser.CmdRetorneContext ctx) {

        output.append("return ");
        // Análise da expressão que está sendo retornada.
        super.visitExpressao(ctx.expressao());
        output.append(";\n");

        return null;
    }


    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {

        String str;
        table = nestedScopes.getCurrentScope();

        // Verifica se é um ponteiro.
        if (ctx.getText().contains("^")) {
            str = "*" + ctx.identificador().getText() + " = " + ctx.expressao().getText() + ";\n";
            output.append(str);
            // Verifica se é um registro.
        } else if (ctx.identificador().getText().contains(".") && ctx.getText().contains("\"")) {
            str = "strcpy(" + ctx.identificador().getText() + "," + ctx.expressao().getText() + ");\n";
            output.append(str);
            // Verifica se é uma variável de um tipo básico.
        } else {
            str = ctx.identificador().getText() + " = " + ctx.expressao().getText() + ";\n";
            output.append(str);
        }

        return null;
    }

    @Override
    public Void visitExpressao(LinguagemAlgoritmicaParser.ExpressaoContext ctx){

        if(ctx.termo_logico().size() > 1){
            for(LinguagemAlgoritmicaParser.Termo_logicoContext logicTerm : ctx.termo_logico()){
                output.append(" || ");
                visitTermo_logico(logicTerm);
            }
        }
        else visitTermo_logico(ctx.termo_logico(0));

        return null;
    }

    @Override
    public Void visitTermo_logico(LinguagemAlgoritmicaParser.Termo_logicoContext ctx){

        if (ctx.fator_logico().size() > 1) {

            for(LinguagemAlgoritmicaParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
                output.append(" && ");
                visitFator_logico(fatorLogico);
            }
        } else
            visitFator_logico(ctx.fator_logico(0));

        return null;

    }

    @Override
    public Void visitFator_logico(LinguagemAlgoritmicaParser.Fator_logicoContext ctx) {

        // Verifica a existência do operador de negação.
        if(ctx.getText().contains("nao"))
            output.append("!");

        visitParcela_logica(ctx.parcela_logica());

        return null;
    }

    @Override
    public Void visitOp2(LinguagemAlgoritmicaParser.Op2Context ctx) {

        output.append(ctx.getText());

        super.visitOp2(ctx);

        return null;
    }

    @Override
    public Void visitParcela_logica(LinguagemAlgoritmicaParser.Parcela_logicaContext ctx) {

        if(ctx.getText().contains("falso"))
            output.append("false");
        else if(ctx.getText().contains("verdadeiro"))
            output.append("true");
        else
            visitExp_relacional(ctx.exp_relacional());

        return null;
    }

    @Override
    public Void visitExp_relacional(LinguagemAlgoritmicaParser.Exp_relacionalContext ctx) {

        String str;
        String actualOP = ctx.getText();
        String actualExp = ctx.exp_aritmetica().get(0).getText();

        // Verifica se o operador atual é o de diferença ou igualdade, para tratá-los
        // de forma especial visto que precisam ser substituídos por seus equivalentes
        // em C.
        if (actualExp.contains("<>"))
            actualOP = "<>";
        else if (actualExp.contains("="))
            if (!actualExp.contains("<=") || !actualExp.contains(">="))
                actualOP = "=";

        if (ctx.op_relacional() != null) {
            output.append(actualExp);
            output.append(ctx.op_relacional().getText());
            output.append(ctx.exp_aritmetica(1).getText());
            // Situação em que precisa tratar dos operadores de igualdade e diferença,
            // ou de uma expressão aritmética.
        } else {
            switch(actualOP) {
                case "=" :
                    String arg1, arg2;
                    arg1 = breakArg(actualExp, 0);
                    arg2 = breakArg(actualExp, 1);
                    str = "(" + arg1;
                    output.append(str);
                    output.append("==");
                    str = arg2 + ")";
                    output.append(str);
                    break;
                case "<>":
                    output.append("!=");
                    break;
                // Caso não seja nenhum dos casos anteriores, trata-se de uma
                // expressão aritmética.
                default:
                    arg1 = breakExp(actualExp, 0);
                    arg2 = breakExp(actualExp, 1);
                    output.append(arg1);
                    String op = verificaOp(actualOP);
                    output.append(op);
                    output.append(arg2);
                    break;
            }
        }

        return null;
    }

    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {

        String str;
        String expressionText;

        expressionText = ctx.expressao().getText().replace("e", "&&");
        expressionText = expressionText.replace("=", "==");

        str = "if (" + expressionText + "){\n";
        output.append(str);

        // Realiza os comandos do if.
        for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmdEntao)
            super.visitCmd(cctx);

        output.append("}\n");

        // Verifica se existe um comando else e o imprime.
        if (ctx.getText().contains("senao")) {

            output.append("else{\n");

            // Realiza os comandos do else.
            for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmdSenao)
                super.visitCmd(cctx);

            output.append("}\n");
        }

        return null;
    }


    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {

        SymbolTable actualScope = scopes.getCurrentScope();
        String nomeVar;
        TypeT5 auxTypeT5;
        String codigoTipo;
        String str;

        // Executa as verificações dos parâmetros atuais.
        for (LinguagemAlgoritmicaParser.IdentificadorContext ictx : ctx.identificador()) {
            nomeVar = ictx.getText();
            auxTypeT5 = actualScope.verify(nomeVar);
            codigoTipo = verifyParamTypeT5(auxTypeT5);

            // Caso seja um literal, imprime a leitura adequada em C.
            if (auxTypeT5 == TypeT5.LITERAL) {
                str = "gets(" + nomeVar + ");\n";
                output.append(str);
                // Impressão dos outros tipos básicos.
            } else {
                str = "scanf(\"%" + codigoTipo + "\",&" + nomeVar + ");\n";
                output.append(str);
            }
        }

        return null;
    }

    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {

        output.append("while(");
        super.visitExpressao(ctx.expressao());
        output.append("){\n");

        // Executa os comandos dentro do while.
        for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);

        output.append("}\n");

        return null;
    }

    @Override
    public Void visitCmdPara(LinguagemAlgoritmicaParser.CmdParaContext ctx) {

        String str;
        String nomeVariavel, limiteEsq, limiteDir;

        nomeVariavel = ctx.IDENT().getText();
        limiteEsq = ctx.exp_aritmetica(0).getText();
        limiteDir = ctx.exp_aritmetica(1).getText();

        // Impressão do comando for com os limites obtidos anteriormente.
        str = "for(" + nomeVariavel + " = " + limiteEsq + "; " + nomeVariavel + " <= " + limiteDir + "; " + nomeVariavel + "++){\n";
        output.append(str);

        // Executa os comandos do for.
        for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmd())
            visitCmd(cctx);

        output.append("}\n");

        return null;
    }

    @Override
    public Void visitCmdFaca(LinguagemAlgoritmicaParser.CmdFacaContext ctx) {

        output.append("do{\n");

        for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);

        output.append("}while(");
        super.visitExpressao(ctx.expressao());
        output.append(");\n");

        return null;
    }

    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {

        TypeT5 auxTypeT5Exp;
        String codTipoExp;

        SymbolTable actualScope = scopes.getCurrentScope();

        for (LinguagemAlgoritmicaParser.ExpressaoContext ectx : ctx.expressao()) {

            String str;

            output.append("printf(\"");

            // Remoção das aspas de uma cadeia passada explicitamente.
            if (ectx.getText().contains("\"")) {
                str = ectx.getText().replace("\"", "") + "\");\n";
                output.append(str);
            } else {
                auxTypeT5Exp = verifyType(actualScope, ectx);
                // Caso seja uma cadeia, imprime com o parâmetro adequado.
                if (auxTypeT5Exp == TypeT5.LITERAL) {
                    str = "%s" + "\", " + ectx.getText() + ");\n";
                    output.append(str);
                    // Caso seja um outro tipo básico, verifica qual é o parâmetro adequado.
                } else {
                    codTipoExp = verifyParamTypeT5(auxTypeT5Exp);
                    str = "%" + codTipoExp + "\", " + ectx.getText() + ");\n";
                    output.append(str);
                }
            }
        }

        return null;
    }

    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {

        String str;
        String limiteEsq, limiteDir;

        str = "switch (" + ctx.exp_aritmetica().getText() + "){\n";
        output.append(str);

        // Executa os blocos do comando Caso.
        for (LinguagemAlgoritmicaParser.Item_selecaoContext sctx : ctx.selecao().item_selecao()) {

            String strOriginal = sctx.constantes().numero_intervalo(0).getText();

            // Obtém os limites esquero e direito do caso atual.
            if (strOriginal.contains(".")) {
                limiteEsq = getCaseLimits(strOriginal, true);
                limiteDir = getCaseLimits(strOriginal, false);
                // Caso seja um valor único, ambos os limites podem receber o mesmo valor.
            } else {
                limiteEsq = getCaseLimits(strOriginal, true);
                limiteDir = getCaseLimits(strOriginal, true);
            }

            if (!sctx.constantes().isEmpty()) {
                for (int i = Integer.parseInt(limiteEsq); i <= Integer.parseInt(limiteDir); i++) {
                    str = "case " + Integer.toString(i) + ":\n";
                    output.append(str);
                }
            } else {
                str = "case " + limiteEsq + ":\n";
                output.append(str);
            }

            for (LinguagemAlgoritmicaParser.CmdContext cctx : sctx.cmd())
                visitCmd(cctx);

            output.append("break;\n");
        }

        output.append("default:\n");

        for (LinguagemAlgoritmicaParser.CmdContext cctx : ctx.cmd())
            visitCmd(cctx);

        output.append("}\n");

        return null;
    }

    @Override
    public Void visitCmdChamada(LinguagemAlgoritmicaParser.CmdChamadaContext ctx) {

        String str;
        str = ctx.IDENT().getText() + "(";
        output.append(str);

        int cont = 0;

        for (LinguagemAlgoritmicaParser.ExpressaoContext ectx : ctx.expressao()) {
            if (cont >= 1)
                output.append(", ");

            output.append(ectx.getText());
            cont += 1;
        }

        output.append(");\n");

        return null;
    }


}
