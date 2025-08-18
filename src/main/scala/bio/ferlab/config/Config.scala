package bio.ferlab.config

final case class AWSConfig(
                            accessKey: String,
                            secretKey: String,
                            endpoint: String,
                            region: String,
                            pathStyleAccess: Boolean,
                            datalakeBucket: String,
                  )

final case class Config(aws: AWSConfig)

