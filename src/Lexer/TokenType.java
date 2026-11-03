package Lexer;

// TokenType (tipo de token): contiene todos los elementos que reconoce el lexer.
public enum TokenType {

    // Arithmetic operators (operadores aritméticos).
    SUM,            // Suma: +
    SUB,            // Resta: -
    MUL,            // Multiplicación: *
    DIV,            // División: /

    // Delimiters (delimitadores).
    LPARENT,        // Paréntesis izquierdo: (
    RPARENT,        // Paréntesis derecho: )
    SEMICOLON,      // Punto y coma: ;
    DOT,            // Punto: .
    COMMA,          // Coma: ,
    COLON,          // Dos puntos: :

    // Assignment and comparisons (asignación y comparaciones).
    ASSIGN,         // Asignación: :=
    EQUAL,          // Igual: =
    LESS_THAN,      // Menor que: <
    GREATER_THAN,   // Mayor que: >
    LESS_EQUAL,     // Menor o igual: <=
    GREATER_EQUAL,  // Mayor o igual: >=
    NOT_EQUAL,      // Diferente de: <>

    // Identifiers and numbers (identificadores y números).
    ID,             // Nombre de una variable o del programa
    INT_LITERAL,    // Número entero
    FLOAT_LITERAL,  // Número real

    // Keywords (palabras reservadas).
    PROGRAM,
    VAR,
    INTEGER,
    REAL,
    BOOLEAN,
    BEGIN,
    END,
    IF,
    THEN,
    ELSE,
    WHILE,
    DO,
    AND,
    OR,
    NOT,
    TRUE,
    FALSE,

    // End of file (fin del código fuente).
    EOF
}
