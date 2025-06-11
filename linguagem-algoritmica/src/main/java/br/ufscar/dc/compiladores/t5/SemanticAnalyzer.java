package br.ufscar.dc.compiladores.t5;


import static br.ufscar.dc.compiladores.t5.SemanticUtils.verifyType;
import static br.ufscar.dc.compiladores.t5.SemanticUtils.addSemanticErrors;
import static br.ufscar.dc.compiladores.t5.SemanticUtils.verificaCompatibilidade;
import static br.ufscar.dc.compiladores.t5.SemanticUtils.mapType;
import br.ufscar.dc.compiladores.t5.SymbolTable.EntryType;
import br.ufscar.dc.compiladores.t5.SymbolTable.TypeT5;
import org.antlr.v4.runtime.Token;
import java.util.ArrayList;
import java.util.HashMap;


//Implementação base dos visitors
public class SemanticAnalyzer extends LinguagemAlgoritmicaBaseVisitor <Void> {

    //tabela de escopos
    SymbolTable tabela;

    //escopos da análise
    static Scopes nestedScopes = new Scopes();


    static HashMap<String, ArrayList<TypeT5>> functionsAndProcedures = new HashMap<>();


    HashMap<String, ArrayList<String>> registerTable = new HashMap<>();

    //adiciona o símbolo alvo à tabela
    public void addTableSymbol(String name, String tipo, Token tokenName, Token TypeT, EntryType TypeE) {

        SymbolTable tabelaLocal = nestedScopes.getCurrentScope();

        TypeT5 itemType;


        if (tipo.charAt(0) == '^')
            tipo = tipo.substring(1);
        //tipos de síbolos
        switch (tipo) {
            case "literal":
                itemType = TypeT5.LITERAL;
                break;
            case "inteiro":
                itemType = TypeT5.INTEGER;
                break;
            case "real":
                itemType = TypeT5.REAL;
                break;
            case "logico":
                itemType = TypeT5.LOGIC;
                break;
            case "void":
                itemType = TypeT5.VOID;
                break;
            case "registro":
                itemType = TypeT5.REGISTER;
                break;
            default:
                itemType = TypeT5.INVALID;
                break;
        }

        //tipo não existente, retorna erro de não dclarado
        if (itemType == TypeT5.INVALID)
            addSemanticErrors(TypeT, "tipo " + tipo + " nao declarado");

        //tipo já existente, retorna erro de declarado anteriormente
        if (!tabelaLocal.exists(name))
            tabelaLocal.add(name, itemType, TypeE);
        else
            addSemanticErrors(tokenName, "identificador " + name + " ja declarado anteriormente");
    }
    //overrides visitors

    @Override
    public Void visitPrograma(LinguagemAlgoritmicaParser.ProgramaContext ctx) {

        for (LinguagemAlgoritmicaParser.CmdContext c : ctx.corpo().cmd())
            if (c.cmdRetorne() != null)
                addSemanticErrors(c.getStart(), "comando retorne nao permitido nesse escopo");

        return super.visitPrograma(ctx);
    }

    @Override
    public Void visitCorpo(LinguagemAlgoritmicaParser.CorpoContext ctx) {
        super.visitCorpo(ctx);

        return null;
    }


    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        String variableType;
        String variableName;


        if (ctx.getText().contains("declare")) {

            if (ctx.variavel().tipo().registro() != null) {

                for (LinguagemAlgoritmicaParser.IdentificadorContext ic : ctx.variavel().identificador()) {
                    addTableSymbol(ic.getText(), "registro", ic.getStart(), null, EntryType.VARIABLE);

                    for (LinguagemAlgoritmicaParser.VariavelContext vc : ctx.variavel().tipo().registro().variavel()) {
                        variableType = vc.tipo().getText();

                        for (LinguagemAlgoritmicaParser.IdentificadorContext icr : vc.identificador())
                            addTableSymbol(ic.getText() + "." + icr.getText(), variableType, icr.getStart(), vc.tipo().getStart(), EntryType.VARIABLE);
                    }
                }

            } else {
                variableType = ctx.variavel().tipo().getText();

                if (registerTable.containsKey(variableType)) {
                    ArrayList<String> registerVariables = registerTable.get(variableType);

                    for (LinguagemAlgoritmicaParser.IdentificadorContext ic : ctx.variavel().identificador()) {
                        variableName = ic.IDENT().get(0).getText();

                        if (tabela.exists(variableName) || registerTable.containsKey(variableName)) {
                            addSemanticErrors(ic.getStart(), "identificador " + variableName + " ja declarado anteriormente");
                        } else {
                            addTableSymbol(variableName, "registro", ic.getStart(), ctx.variavel().tipo().getStart(), EntryType.VARIABLE);

                            for (int i = 0; i < registerVariables.size(); i = i + 2) {
                                addTableSymbol(variableName + "." + registerVariables.get(i), registerVariables.get(i+1), ic.getStart(), ctx.variavel().tipo().getStart(), EntryType.VARIABLE);
                            }
                        }
                    }

                } else {
                    for (LinguagemAlgoritmicaParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                        variableName = ident.getText();


                        if (functionsAndProcedures.containsKey(variableName))
                            addSemanticErrors(ident.getStart(), "identificador " + variableName + " ja declarado anteriormente");
                        else
                            addTableSymbol(variableName, variableType, ident.getStart(), ctx.variavel().tipo().getStart(), EntryType.VARIABLE);
                    }
                }
            }

        } else if (ctx.getText().contains("tipo")) {

            if (ctx.tipo().registro() != null) {
                ArrayList<String> registerVariables = new ArrayList<>();

                for (LinguagemAlgoritmicaParser.VariavelContext vc : ctx.tipo().registro().variavel()) {
                    variableType = vc.tipo().getText();

                    for (LinguagemAlgoritmicaParser.IdentificadorContext ic : vc.identificador()) {
                        registerVariables.add(ic.getText());
                        registerVariables.add(variableType);
                    }
                }
                registerTable.put(ctx.IDENT().getText(), registerVariables);
            }

        } else if (ctx.getText().contains("constante"))
            addTableSymbol(ctx.IDENT().getText(), ctx.tipo_basico().getText(), ctx.IDENT().getSymbol(), ctx.IDENT().getSymbol(), EntryType.VARIABLE);

        return super.visitDeclaracao_local(ctx);
    }

    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {

        nestedScopes.createNewScope();

        tabela = nestedScopes.getCurrentScope();


        ArrayList<TypeT5> variablesTypes = new ArrayList<>();
        ArrayList<String> registerVariables;

        String variableType;
        TypeT5 auxType;

        if (ctx.getText().contains("procedimento")) {

            for (LinguagemAlgoritmicaParser.ParametroContext parametro : ctx.parametros().parametro()) {

                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {

                    addTableSymbol(parametro.identificador().get(0).getText(), parametro.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), parametro.getStart(), parametro.getStart(), EntryType.VARIABLE);


                    variableType = parametro.tipo_estendido().getText();
                    auxType = mapType(registerTable, variableType);
                    variablesTypes.add(auxType);

                } else if (registerTable.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {

                    registerVariables = registerTable.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());


                    variableType = parametro.tipo_estendido().getText();
                    auxType = mapType(registerTable, variableType);
                    variablesTypes.add(auxType);

                    for (LinguagemAlgoritmicaParser.IdentificadorContext ic : parametro.identificador())

                        for (int i = 0; i < registerVariables.size(); i = i + 2)
                            addTableSymbol(ic.getText() + "." + registerVariables.get(i), registerVariables.get(i + 1), ic.getStart(), ic.getStart(), EntryType.VARIABLE);
                } else
                    addSemanticErrors(parametro.getStart(), "tipo nao declarado");
            }
            for (LinguagemAlgoritmicaParser.CmdContext c : ctx.cmd())
                if (c.cmdRetorne() != null)
                    addSemanticErrors(c.getStart(), "comando retorne nao permitido nesse escopo");

            functionsAndProcedures.put(ctx.IDENT().getText(), variablesTypes);

        } else if (ctx.getText().contains("funcao")) {
            for (LinguagemAlgoritmicaParser.ParametroContext parametro : ctx.parametros().parametro()) {

                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {

                    addTableSymbol(parametro.identificador().get(0).getText(), parametro.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), parametro.getStart(), parametro.getStart(), EntryType.VARIABLE);

                    variableType = parametro.tipo_estendido().getText();
                    auxType = mapType(registerTable, variableType);
                    variablesTypes.add(auxType);
                } else if (registerTable.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {

                    registerVariables = registerTable.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());

                    variableType = parametro.tipo_estendido().tipo_basico_ident().IDENT().getText();
                    auxType = mapType(registerTable, variableType);
                    variablesTypes.add(auxType);

                    for (LinguagemAlgoritmicaParser.IdentificadorContext ic : parametro.identificador())
                        for (int i = 0; i < registerVariables.size(); i = i + 2)
                            addTableSymbol(ic.getText() + "." + registerVariables.get(i), registerVariables.get(i + 1), ic.getStart(), ic.getStart(), EntryType.VARIABLE);
                } else
                    addSemanticErrors(parametro.getStart(), "tipo nao declarado");
            }

            functionsAndProcedures.put(ctx.IDENT().getText(), variablesTypes);
        }

        super.visitDeclaracao_global(ctx);

        nestedScopes.leaveScope();

        if (ctx.getText().contains("procedimento"))
            addTableSymbol(ctx.IDENT().getText(), "void", ctx.getStart(), ctx.getStart(), EntryType.PROCEEDING);
        else if (ctx.getText().contains("funcao"))
            addTableSymbol(ctx.IDENT().getText(), ctx.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), ctx.getStart(), ctx.getStart(), EntryType.FUNCTION);

        return null;
    }

    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        for (LinguagemAlgoritmicaParser.IdentificadorContext id : ctx.identificador())
            if (!tabela.exists(id.getText()))
                addSemanticErrors(id.getStart(), "identificador " + id.getText() + " nao declarado");

        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        TypeT5 tipo;

        for (LinguagemAlgoritmicaParser.ExpressaoContext expressao : ctx.expressao())
            tipo = verifyType(tabela, expressao);

        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        TypeT5 tipo = verifyType(tabela, ctx.expressao());

        return super.visitCmdEnquanto(ctx);
    }

    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        TypeT5 tipo = verifyType(tabela, ctx.expressao());

        return super.visitCmdSe(ctx);
    }

    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        tabela = nestedScopes.getCurrentScope();

        TypeT5 tipoExpressao = verifyType(tabela, ctx.expressao());

        String varNome = ctx.identificador().getText();

        if (tipoExpressao != TypeT5.INVALID) {
            // Caso a variável não tenha sido declarada, informa o erro.
            if (!tabela.exists(varNome))
                addSemanticErrors(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
            else {
                TypeT5 varTipo = verifyType(tabela, varNome);
                if (varTipo == TypeT5.INTEGER || varTipo == TypeT5.REAL) {
                    if (ctx.getText().contains("ponteiro")) {
                        if (!verificaCompatibilidade(varTipo, tipoExpressao))
                            if (tipoExpressao != TypeT5.INTEGER)
                                addSemanticErrors(ctx.identificador().getStart(), "atribuicao nao compativel para ^" + ctx.identificador().getText());
                    } else if (!verificaCompatibilidade(varTipo, tipoExpressao))
                        if (tipoExpressao != TypeT5.INTEGER)
                            addSemanticErrors(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
                } else if (varTipo != tipoExpressao)
                    addSemanticErrors(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
            }
        }

        return super.visitCmdAtribuicao(ctx);
    }

}