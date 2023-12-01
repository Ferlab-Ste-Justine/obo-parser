package bio.ferlab.config

final case class AWSConfig(
                    accessKey: String,
                    secretKey: String,
                    endpoint: String,
                    region: String,
                    pathStyleAccess: Boolean,
                    bucketName: String,
                  )

final case class Config(aws: AWSConfig)

