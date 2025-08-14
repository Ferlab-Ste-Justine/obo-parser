# obo-parser

The Obo Parser is build on Scala and Spark


## Build
Make sure to import all external libraries by running 
```
`sbt update`. 
```

To build the application, run the following from the command line in the root directory of the project

```
sbt ";clean;assembly"
```

Then get the application jar file from

```
${root of the project}/target/scala-2.12/obo-parser-assembly-0.1.0-SNAPSHOT.jar
```

- Spark 3.0.0

## Command line arguments

Two arguments is required to executing a build with Obo Parser : `obo_file_input_url json_output_local_path `.
`obo_file_input_url` is the URL or local path of the input file. 
`json_output_local_path` is the local path where the json output will be saved

```
${SPARK_HOME}/bin/spark-submit --master ${spark_master} --class ${full_main_class_name} ./obo-parser/target/scala-2.12/obo-parser-assembly-0.1.0-SNAPSHOT.jar ${obo_file_input_url} ${json_output_local_path}
```

## Arguments (Mandatory)

1st argument: Path to fetch input obo file 
    
- https://purl.obolibrary.org/obo/hp.obo
- https://purl.obolibrary.org/obo/mondo.obo
- https://purl.obolibrary.org/obo/ncit.obo

2nd argument: Type of ontological terms, values can be one of those: `hpo` or `mondo` or `ncit` or `icd`.

3rd argument (optional): ex. MONDO:0700096. 

For MONDO terms, a desired to node should be provided. MONDO has many top nodes, so we need to specify which one we want to use.
the desired top node for MONDO, it should be MONDO:0700096 (human disease). Using that as a desired top node we remove
the same level branch MONDO:0005583 (non-human animal disease). We should get:
MONDO:0000001 (disease) -> MONDO:0700096 (human disease) -> {REST of Mondo tree}
