package IntermediateCode;

import Lexer.TokenType;

// TacInstruction (instrucción TAC): representa un cuádruplo y su tipo de resultado.
// Los saltos y las etiquetas también se representan como instrucciones.
public class TacInstruction {
    private final String op;
    private final String arg1;
    private final String arg2;
    private final String result;
    private final TokenType type;

    // TacInstruction (constructor): guarda la operación, los operandos y el destino.
    public TacInstruction(String op, String arg1, String arg2, String result, TokenType type) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
        this.type = type;
    }

    // getOp (operación): identifica la operación del cuádruplo.
    public String getOp() {
        return op;
    }

    // getArg1 (primer argumento): devuelve el operando izquierdo o único.
    public String getArg1() {
        return arg1;
    }

    // getArg2 (segundo argumento): devuelve el segundo operando cuando existe.
    public String getArg2() {
        return arg2;
    }

    // getResult (resultado): devuelve la variable temporal, destino o etiqueta.
    public String getResult() {
        return result;
    }

    // getType (tipo): permite distinguir operaciones INTEGER, REAL y BOOLEAN.
    // Es null para instrucciones de salto o etiquetas.
    public TokenType getType() {
        return type;
    }

    // toString (mostrar): convierte el cuádruplo a una línea TAC legible.
    @Override
    public String toString() {
        switch (op) {
            case "LABEL":
                return result + ":";
            case "GOTO":
                return "goto " + result;
            case "IF_TRUE":
                return "if " + arg1 + " goto " + result;
            case "ASSIGN":
                return result + " = " + arg1;
            case "I2R":
                return result + " = I2R " + arg1;
            case "NEG":
                return result + " = -" + arg1;
            default:
                return result + " = " + arg1 + " " + op + " " + arg2;
        }
    }
}
