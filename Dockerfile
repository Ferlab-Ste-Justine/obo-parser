FROM apache/spark:3.5.1

COPY target/scala-2.12/obo-parser.jar /app/obo-parser.jar
