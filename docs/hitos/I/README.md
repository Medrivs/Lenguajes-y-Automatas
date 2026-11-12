# Hito I — Generación de ensamblador NASM x86-64

## Objetivo

Traducir el **TAC optimizado del Hito H** a código ensamblador **NASM para Linux x86-64**. El compilador sigue escrito en **Java 21** y se puede ejecutar desde **Apache NetBeans**. La salida se guarda como `build/programa.asm`.

Este hito **genera el código ensamblador**; ensamblarlo y ejecutarlo en Linux con NASM y el enlazador `ld` es un paso externo opcional. Pascal Subset v1.0 no incluye `WRITE`, `WRITELN` ni lectura de datos, por lo que el ejecutable final **no imprime resultados** y termina con código de salida `0`.

## Flujo completo

```text
Código Pascal
  → Lexer → Parser → AST
  → SymbolTable → SemanticAnalyzer (tipos y símbolos)
  → AstVisitor / AST decorado
  → TacGenerator → Lista de cuádruplos TAC
  → TacOptimizer → TAC optimizado
  → NasmGenerator → NASM Linux x86-64
  → build/programa.asm
  → [externo: nasm + ld → ejecutable Linux]
```

## Archivos nuevos y modificados

| Archivo | Responsabilidad |
|---|---|
| `src/ObjectCode/NasmGenerator.java` | Nuevo. Traduce instrucciones TAC a secciones, datos, registros y saltos NASM. |
| `test/ObjectCode/NasmGeneratorTest.java` | Nuevo. 111 comprobaciones automáticas de generación, tipos y validaciones. |
| `src/TestMain/Main.java` | Modificado. Imprime NASM y guarda `build/programa.asm`. |
| `docs/hitos/I/README.md` | Nuevo. Documentación técnica del Hito I. |
| `README.md` | Modificado. Enlace a este hito e instrucciones actualizadas. |

No se modifica la gramática, el análisis semántico, el Visitor ni las reglas del optimizador.

## Relación con el material del profesor

El documento **`Código Objeto.pdf`**, páginas 2–10, muestra un programa Linux x86-64 con sintaxis NASM, `section .data`, `section .text`, `global _start` y la terminación mediante `mov eax, 60`, `xor edi, edi`, `syscall`. Su ejemplo calcula el área de un círculo utilizando la FPU x87 (`fld`, `fmul`, `fstp`).

Adoptamos el **formato NASM y el modelo de programa Linux**; para implementar los operadores tipados de nuestro compilador se utilizan **SSE2 (`xmm0`, `xmm1`) para `REAL`**, con `movsd`, `addsd`, `subsd`, `mulsd`, `divsd` y `ucomisd`. Esto es una **adaptación**, no una copia literal de las instrucciones del ejemplo.

Los ejemplos de **`Cuádruplo.pdf`** y **`Optimización-TAC.txt`** orientan la representación y la optimización que ya fueron implementadas en los hitos G y H.

## Modelo de memoria y tipos

| Tipo Pascal | Almacenamiento | Operación principal |
|---|---|---|
| `INTEGER` | `resd 1` — 32 bits con signo | `mov eax`, `add`, `sub`, `imul`, `cmp` |
| `REAL` | `resq 1` — 64 bits IEEE 754 | `movsd`, `addsd`, `subsd`, `mulsd`, `divsd` |
| `BOOLEAN` | `resd 1` — 32 bits, `0` o `1` | `mov`, `test`, `jnz`, `set*` |

Las variables se declaran en `section .bss` y comienzan en cero; los literales reales y la máscara de signo se almacenan en `section .rodata`. Las instrucciones van en `section .text`.

Para evitar colisiones con nombres de registros o palabras reservadas del ensamblador, usamos prefijos propios:

```text
counter    → asm_v_COUNTER
_t1        → asm_tmp_1
L1         → asm_L1
3.14       → asm_real_const_1
```

Los nombres Pascal se comparan sin distinguir mayúsculas/minúsculas, como en la tabla de símbolos.

## Operaciones traducidas

| Operación TAC | Traducción NASM |
|---|---|
| `ASSIGN` | Carga el operando y lo guarda en la variable o temporal. |
| `+`, `-`, `*` de `INTEGER` | `add`, `sub`, `imul` con registros de 32 bits. |
| `+`, `-`, `*`, `/` de `REAL` | `addsd`, `subsd`, `mulsd`, `divsd` con registros SSE2. |
| `NEG` | `neg eax` para enteros; cambio del bit de signo con `xorpd` para reales. |
| `I2R` | `cvtsi2sd xmm0, eax` convierte INTEGER a REAL. |
| `==`, `!=`, `<`, `<=`, `>`, `>=` | `cmp` + `set*` para INTEGER/BOOLEAN; `ucomisd` + `set*` para REAL. |
| `IF_TRUE` | Carga la condición, hace `test eax, eax` y `jnz asm_Ln`. |
| `GOTO` | `jmp asm_Ln`. |
| `LABEL` | Emite `asm_Ln:`. |

**Importante:** la división Pascal `/` siempre produce `REAL`. No se genera `idiv` para `/` entre enteros; en el TAC esas entradas ya contienen las conversiones `I2R`. El generador acepta también una conversión directa al cargar un entero como operando real.

### Comparaciones de números reales

`ucomisd` compara valores `double`; para `==`, `!=`, `<` y `<=` se combinan los bits de estado cuando es necesario para tratar correctamente el caso `NaN` (no ordenado). Se mantienen las comparaciones con signo en enteros (`setl`, `setle`, `setg`, `setge`).

### Condiciones y ciclos

No se vuelven a analizar `IF`, `WHILE`, `AND` ni `OR` en esta fase. **El TAC ya contiene `LABEL`, `GOTO` e `IF_TRUE`** con cortocircuito, por lo que `NasmGenerator` traduce los saltos en el mismo orden y conserva el comportamiento del flujo.

Ejemplo aproximado para `WHILE counter < 3 DO counter := counter + 1;`:

```asm
asm_L1:
    mov eax, dword [rel asm_v_COUNTER]
    mov ebx, 3
    cmp eax, ebx
    setl al
    movzx eax, al
    mov dword [rel asm_tmp_1], eax
    mov eax, dword [rel asm_tmp_1]
    test eax, eax
    jnz asm_L2
    jmp asm_L3
asm_L2:
    ; Instrucciones de incremento ...
    jmp asm_L1
asm_L3:
```

Las etiquetas reales y números temporales pueden variar según el orden del programa y las optimizaciones.

## Cómo funciona `NasmGenerator.java`

- `generate(instructions, table)`: limpia el estado, registra variables y temporales, verifica etiquetas, traduce los cuádruplos y construye `section .rodata`, `.bss` y `.text`.
- `registerVariables()`: obtiene los símbolos globales y sus tipos desde `SymbolTable`.
- `registerTemporaries()`: registra los `_tN` y comprueba que su tipo sea consistente.
- `checkLabels()`: comprueba que no haya destinos de salto inexistentes ni etiquetas duplicadas.
- `emitInstruction()`: realiza un `switch` con las operaciones TAC admitidas.
- `emitAssignment()`, `emitNegation()`, `emitArithmetic()`, `emitComparison()`: traducen cálculos y operaciones.
- `loadInteger()`, `loadReal()`: cargan constantes o variables en registros apropiados.
- `storeInteger()`, `storeReal()`: escriben resultados en memoria.
- `operandType()`, `destinationType()`: consultan tipos de variables, temporales y constantes.
- `getText()`, `writeFile(Path)`: obtienen o exportan el ensamblador en UTF-8.

Las excepciones producidas por TAC inconsistente están escritas en español y comienzan por `Error NASM:`.

## Usar desde NetBeans sin terminal

1. Abre el proyecto con JDK 21 y ejecuta **Clean and Build**.
2. Ejecuta la clase `TestMain.Main`.
3. Observa en la consola la sección **CÓDIGO OBJETO NASM x86-64**.
4. Busca el archivo generado `build/programa.asm` en la carpeta física del proyecto. Es un archivo de salida, no código fuente.
5. Ejecuta `ObjectCode.NasmGeneratorTest` con **Run File**.

La ruta del `.asm` también se muestra en la consola, para que puedas localizarla sin problemas desde el Explorador de Windows.

## Ensamblar y enlazar en Linux o WSL (opcional)

Si tienes instalado **NASM** y el enlazador binario `ld` en Linux x86-64 (o Ubuntu dentro de WSL), desde la raíz del proyecto puedes ejecutar:

```bash
nasm -f elf64 build/programa.asm -o build/programa.o
ld build/programa.o -o build/programa
./build/programa
echo $?
```

El código de salida esperado es `0`. **No habrá texto en pantalla**, porque el lenguaje no tiene `WRITE` ni una operación TAC para imprimir. Para inspeccionar el valor de una variable haría falta una depuración adicional o agregar salida en un hito futuro, lo cual no forma parte del contrato v1.0.

**NetBeans en Windows puede generar el `.asm`, pero no ejecuta NASM/Linux automáticamente.** La traducción Java no necesita NASM instalado.

## Pruebas y validación

| Hito | Comprobaciones |
|---|---:|
| B — Lexer | 104 |
| C — Parser | 140 |
| D — Tabla de símbolos | 38 |
| E — Semántica | 70 |
| F — Visitor | 61 |
| G — TAC | 74 |
| H — Optimizador | 98 |
| **I — NASM** | **111** |
| **Total acumulado** | **696** |

El proyecto se compiló con Java 21/Ant y pasaron todas las pruebas anteriores. Además se ejecutaron **38 comprobaciones de comportamiento x86-64** (19 programas en TAC original y optimizado) utilizando GNU `as` y `ld`, después de una conversión de sintaxis de prueba desde NASM a GAS. **Estas verificaciones no sustituyen una ejecución directa con NASM**, que no estaba instalado en el entorno de desarrollo de esta entrega. Las comprobaciones Java sí revisan la estructura NASM, las instrucciones, tipos, etiquetas y exportación a archivo.

## Límites y siguientes pasos

- Plataforma: **Linux x86-64**, no ensamblador ejecutable directamente en Windows.
- Solo se generan operaciones existentes en Pascal Subset v1.0; no hay funciones, arreglos, cadenas, entrada ni salida.
- No se implementan convenciones de llamada a funciones ni biblioteca estándar: el punto de entrada es `_start` y se termina con `sys_exit`.
- Los `INTEGER` operan con aritmética de 32 bits; el contrato no define un control de desbordamiento en ejecución.
- Las excepciones de punto flotante se dejan bajo el comportamiento IEEE 754 de SSE2; no se genera una comprobación explícita de división por cero.
- Próximo Hito J: pruebas integradas y comprobación final de todos los módulos, incluyendo NASM/WSL cuando esté disponible.

## Commit recomendado

`feat(nasm): generar ensamblador Linux x86-64 desde TAC optimizado`
