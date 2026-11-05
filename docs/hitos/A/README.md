# Hito A — Gramática y contrato del lenguaje

## Objetivo

Definir exactamente qué puede escribir el usuario en **Pascal Subset v1.0**, antes de modificar el lexer y el parser. La gramática oficial está en [`src/Grammar/PascalSubset_v1.0.ebnf`](../../../src/Grammar/PascalSubset_v1.0.ebnf).

## Estructura del programa

```pascal
PROGRAM Demo;
VAR
    counter: INTEGER;
    ready: BOOLEAN;
BEGIN
    counter := 0;
    ready := TRUE;
END.
```

La producción principal es `program → PROGRAM ID ; block . EOF`, y `block → declaration_part statement_part`.

## Funcionalidades incluidas

- Variables globales `INTEGER`, `REAL` y `BOOLEAN`; listas de identificadores en una declaración.
- Asignación con `:=`; bloques `BEGIN ... END`, que pueden estar vacíos o anidados.
- `IF ... THEN ... [ELSE ...]` y `WHILE ... DO ...`.
- Expresiones con `+`, `-`, `*`, `/`, `AND`, `OR`, `NOT` y comparaciones.
- Literales enteros, reales y booleanos.
- Identificadores `[A-Za-z][A-Za-z0-9_]*`, sin distinción entre mayúsculas y minúsculas.

## Decisiones importantes

- Todas las declaraciones están al inicio, dentro de una única sección `VAR` opcional.
- Cada declaración termina en `;`; se permite un `;` final antes de `END`, pero no `;;`.
- `ELSE` pertenece al `IF` pendiente más cercano; no se escribe `;` inmediatamente antes de `ELSE`.
- `AND` comparte precedencia con `*` y `/`; `OR` con `+` y `-`. Para combinar comparaciones, usa paréntesis: `(a < b) AND (c < d)`.
- La división `/` siempre resulta en `REAL`; se permite conversión implícita `INTEGER → REAL`, no la inversa.
- Se prohíben nombres de variables duplicados y reutilizar el nombre del programa.
- Las variables deben estar inicializadas antes de leerse. Esa validación se implementa en el análisis semántico.

## Fuera del alcance

No se admiten funciones, procedimientos, arreglos, cadenas, `FOR`, `REPEAT`, entrada/salida ni comentarios en la versión 1.0. Los bloques interiores no crean ámbitos de declaración.

## Resultado

Este hito es la referencia para el resto del proyecto: **las siguientes fases deben respetar la gramática**, sin inventar instrucciones nuevas.
