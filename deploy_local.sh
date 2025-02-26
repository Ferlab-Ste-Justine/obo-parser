#!/bin/sh

VERSION="${1:-latest}"
ENV="${2:-qa}"
USAGE="<version> <env=[qa|staging|prod](default=qa)>"

showUsage()
{
  echo "Usage:"
  echo "  sh ./deploy.sh $USAGE"
  echo "  ./deploy.sh $USAGE (required: chmod +x ./launch.sh)"
  echo
}

if [ -z "$VERSION" ]
  then
    showUsage
    exit 1
fi

echo "=== DOWNLOAD FROM GITHUB ==="
sbt clean assembly

# copy + list S3, update profile name if necessary
echo "=== COPY TO S3 ($ENV) ==="
aws --profile cqgc-$ENV --endpoint https://s3.cqgc.hsj.rtss.qc.ca s3 cp target/scala-2.12/obo-parser.jar s3://cqgc-$ENV-app-datalake/jars/obo-parser-$VERSION.jar

echo "=== JARS IN S3 ($ENV) ==="
aws --profile cqgc-$ENV --endpoint https://s3.cqgc.hsj.rtss.qc.ca s3 ls s3://cqgc-$ENV-app-datalake/jars --recursive