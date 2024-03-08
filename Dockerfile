FROM apache/spark:3.4.2

COPY target/scala-2.12/obo-parser.jar /app/obo-parser.jar
