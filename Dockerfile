FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y git llvm clang

WORKDIR /app

RUN curl -o antlr-4.13.2-complete.jar https://www.antlr.org/download/antlr-4.13.2-complete.jar

RUN rm -rf /app/test
RUN git clone https://64361:glpat-jxvHn_GayPTj7vUepxoz@git.alunos.di.fc.ul.pt/tcomp000/aguda-testing.git /app/test

COPY . .

RUN java -cp /app/antlr-4.13.2-complete.jar org.antlr.v4.Tool -visitor -o /app/src/aguda/parser -package aguda.parser /app/src/aguda/parser/Aguda.g4

RUN javac -cp /app/antlr-4.13.2-complete.jar:/app/src:/app/app -d /app/app /app/app/Main.java /app/src/aguda/parser/*.java /app/src/aguda/ast/*.java /app/src/aguda/checker/*.java /app/src/aguda/context/*.java /app/src/aguda/types/*.java /app/src/aguda/codegen/*.java

RUN chmod +x /app/test/test-syntax.sh
RUN chmod +x /app/test/test-semantic.sh
RUN chmod +x /app/test/test-codegen.sh