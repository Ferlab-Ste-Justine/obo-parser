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