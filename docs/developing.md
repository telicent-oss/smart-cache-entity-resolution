# Developing

## Code Style

This repository includes an [`.editorconfig`](https://editorconfig.org) file that provides a minimal code style
specification for the repository primarily covering indentation and line width. This has been generated from an
IntelliJ code style so includes IntelliJ specific properties that will be interpreted and applied by IntelliJ. If you
are using a different IDE you may need to obtain a plugin for the `.editorconfig` file to be honoured by your IDE and
the IntelliJ specific properties may be ignored.

There are also an `intellij-code-style.xml` and `eclipse-code-style.xml` configuration files included in this repository
which provide IDE specific code formatting configurations. These are more complete than the `.editorconfig` so
developers **SHOULD** import the relevant file for their IDE and configure their project to use this style.

For Intellij in particular you can set up the config by going to Settings -> Editor -> Code Style and importing the
schema.

Note that the `.editorconfig` rules are validated during the `verify` phase of Maven builds and any violations will
cause a build failure. The Maven plugin used can automatically fix some violations by running `mvn
editorconfig:format`.

## Ignoring formatting

You may wish to use `//@formatter:off` and `//@formatter:on` comments to fence code blocks whose formatting
you wish to preserve, e.g., where the IDEs automatic formatting makes it harder to read.


## Debugging / Running in IntelliJ

### Canonical Index Command
To run the Canonical Index Command, which populates the relevant ElasticSearch Index with data from a given Kafka 
Topic, you should review the entries in `.idea/runConfigurations`. The configuration file sets the following command 
line parameters:

| Parameter           | Value                     | Notes                                                            |
|---------------------|---------------------------|------------------------------------------------------------------|
| bootstrap-server    | localhost:19093           | Kafka Instance                                                   |
| elastic-host        | localhost                 | The ES Instance                                                  | 
| canonical-config    | dynamic_config_sample.yml | Canonical configuration instances                                |
| topic               | canonical_boxers          | Kafka Topic to read from                                         |
| index               | canonical_boxers          | ES Index to populate                                             |
| index-configuration | Boxers                    | Name of the Canonical Configuration to use when setting up index |

It assumes that the Kafka & ElasticSearch instances are running locally.


### Entity Resolution API Server
To run the ER API Server, you should see "EntityResolutionApiCommand" in the run configurations. The configuration 
file in question is found in `.idea/runConfigurations` and sets the following variables.

| Environment Variable | Value     | Note                            |
|----------------------|-----------|---------------------------------|
| ELASTIC_HOST         | localhost |                                 |
| ELASTIC_PORT         | 9200      |                                 |
| ELASTIC_INDEX        | canonical |                                 |
| JWKS_URL             | disabled  | Disables JWT Authentication     |
| USER_ATTRIBUTES_URL  | disabled  | Disables User Attribute look-up |

It assumes that the various CORE/Elastic components are running locally. 

