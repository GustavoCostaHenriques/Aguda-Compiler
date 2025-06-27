# Aguda Compiler — Milestone 4

## 🛠️ How to Build the Compiler

The project uses Java and ANTLR 4. To compile the project:

1. Ensure Java 17+ and ANTLR 4.13.2 are available.
2. Use the `Dockerfile` provided to build the container:

```bash
docker-compose up --build
```

This will automatically set up everything, including ANTLR and the classpath for compilation.

⚠️ Note: If by any chance after you run the command the following message appears and you can't run another command, just click with your mouse in any place outside of the terminal and you will be able to proceed to the next commands.


```bash
aguda-1 exited with code 0
```

After this command do:

```bash
docker-compose run --rm aguda bash
```

This will create a shell inside the container so that we can now test everything!
---

## 🦢 How to Run a Particular Test

You can run a single `.agu` test file by passing it to the main program like this:

```bash
java -cp /app/antlr-4.13.2-complete.jar:/app/src:/app/app app.Main /app/test/test/valid/54394_clamp/clamp.agu --test-execution 3
```

In the test above it was tested the exeuction of the **valid/54394_clamp/clamp.agu** test file. The number 3 represents the maximum number of erros that will be shown, by default, the maximum number of erros is 10. 

Optional flags available:

- `--ast` — prints only the AST.
- `--parser` — prints only the parse tree.
- `--test-syntax <max_number_of_errors>` — used in test syntax mode to return only pass/fail and error diagnostics.
- `--test-semantic <max_number_of_errors>` — used in test semantic mode to return only pass/fail and error diagnostics.
- `--test-execution <max_number_of_errors>` — used in test code generation mode to return pass/fail, the output and error diagnostics.

---

Example with AST:

```bash
java -cp /app/antlr-4.13.2-complete.jar:/app/src:/app/app app.Main /app/test/test/valid/54394_clamp/clamp.agu --ast
```

---

## 📂 How to Run the Whole Test Suite

From the root of the project, run the following command in the app directory to test the syntax of all the files:

```bash
./test/test-syntax.sh <max_number_of_errors>
```

To test the semantic of all the files:

```bash
./test/test-semantic.sh <max_number_of_errors>
``` 

This will execute all the test files located under the `test/test` directory, across these categories:

- `test/test/valid`
- `test/test/invalid-syntax`
- `test/test/invalid-semantic`

To test the code generation of all the files:

```bash
./test/test-codegen.sh <max_number_of_errors>
```

This will execute all the test files located under the `test/test` directory, across these categories:

- `test/test/valid`

It automatically discovers tests in subdirectories and handles each test individually. And it generates a file in the logs directory specifying the kind of test that was done (sintactic, semantic or execution) and the date. In case it was tested the execution it will be created a **.ll** file and an **.out** file along side with the **.agu** file and **.expect** file that already exist in the test directory.

If there was some mistake there will be a file **.err** with the diagnostic.

---

## 📊 How to Interpret the Testing Output

The output of the script provides:

- ✅ Tests that passed
- ❌ Tests that failed
- 📋 A summary of totals per category (valid, invalid-syntax, invalid-semantic)
- 📜 A file with full output

At the end of the test, you'll see something like this in the console:

```
=============================== 📋
✅ SUMMARY
📊 TOTAL: 42 | ✅ PASSED: 40 | ❌ FAILED: 2
📜 Report written to test/test.log
```

And inside the new file in the logs directory, failed tests are detailed like this:

```
Tests Failed ❌:

> invalid-syntax/46494_missing_paren/missing_paren.agu
--------------------------------
Test failed ❌
Syntactic error: line 4, column 27
Description: missing ')' at 'then'
>> if x > 3 then print("hello"
                         ^
```

---

## 🧠 How to Distinguish Errors

In `--test` mode, the compiler distinguishes between:

- **Lexical errors** — printed as: `Lexical error: line X, column Y`
- **Syntactic errors** — printed as: `Syntactic error: line X, column Y`
- **Semantic errors** — printed as: `Error: (line X, column Y) description`
- **Execution errors** — printed as: `Not implemented: Generation code for (line X, column Y) expression 'expression'`


Example of a lexical error:

```
Test failed ❌
Lexical error: line 2, column 6
Description: invalid character: '@'
>> let x@ = 4;
        ^
```

Example of a syntactic error:

```
Test failed ❌
Syntactic error: line 5, column 14
Description: mismatched input 'then' expecting expression
>> if x > 3 then
              ^
```

Example of a semantic error:

```

Test Failed ❌
Error: (11, 23) Function 'sumArray' expects argument of type Int[], found Int[][]
↳  let _ : Int = sumArray(new Int[] [5 | 10])
                          ^

```

Example of an execution error:

```

Not implemented: Generation code for (3,0) expression 'let bottomRow(n ) : Int -> Int[][] ='

```

If an invalid-syntax test does **not** produce an error as expected, the output will clearly show:

```
⚠️  Expected an error but none was thrown.
```

If the semantics are being teste the same thing happens in the invalid-syntax case, but also in the invalid-semantic case.

---

**Author:** Gustavo Henriques — 64361

