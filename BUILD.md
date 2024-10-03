# Telicent Smart Cache - Entity Resolution - Build Instructions

# Requirements

This project is Java based and built with Maven. You will require the following to build it:

- JDK 21+
- [Apache Maven](https://maven.apache.org) 3.8.1+

This project has dependencies on other Telicent Maven packages which are published to GitHub at
https://github.com/telicent-oss

# Build

Assuming you have met the above requirements you can build like so:

```bash
$ mvn clean install
```

This will build all the Maven packages for this repository.

## SNAPSHOT Builds

Maven Snapshots are only published from `main`, or when a developer manually runs `mvn deploy` from their machine.

## Release Builds

Release builds should be carried out using the normal Maven release process from a developers local machine.

### Preparing for a Release

Firstly, make sure `main` is up-to-date and create a new branch from it for the release:

```bash
$ git checkout main
$ git pull
$ git checkout -B release/<version>
$ git push -u origin release/<version>
```

### Maven Release Preparation

Then you can prepare a Maven release, we use the `-D` arguments to specify our desired versions and tags to avoid the
developer being prompted for every module:

```bash
$ mvn release:clean release:prepare -DreleaseVersion=<version> -DdevelopmentVersion=<next-version> -Dtag=<version>
```

Where `<version>` is the desired release version e.g. `0.10.1` and `<next-version>` is the new development version e.g.
`0.10.2-SNAPSHOT`.  This step includes running the tests so will take some time, if the release preparation fails please
refer to [Recovering from a Release Preparation Failure](#recovering-from-a-release-preparation-failure) for how to
proceed.

After this step completes successfully you should see two commits in the `git log` output e.g.

```bash
$ git log --oneline
61b5db2 (HEAD -> release/0.10.1-rc6, origin/release/0.10.1-rc6) [maven-release-plugin] prepare for next development iteration
3a9f182 (tag: 0.10.1) [maven-release-plugin] prepare release 0.10.1
```

### Recovering from a Release Preparation Failure

Sometimes release preparation may fail, for example you might have uncommitted changes in your working copy, you might
encounter a transient test failure, a networking error resolving dependencies etc.  In this case you will need to
recover from this before you can retry the release.

The first thing to do is check whether the release plugin made any commits (see the `git log --oneline` example in the
previous section).

#### Recovering when Commits Exist

If you have one/both commits present, and more specifically if they have been pushed to the remote
repository, then you will need to explicitly roll back the release:

```bash
$ mvn release:rollback
```

This should create a new commit that undoes all the release changes and preparation your prior release preparation
attempt generated.  You should then have a fresh commit on your release branch that undid the release preparation.  Once
that is present you can address any underlying issues on your branch before attempting the [Maven Release
Preparation](#maven-release-preparation) again.  You will also need to check whether you need to [Delete the
tag](#recovering-when-the-tag-was-created) before retrying.

#### Recovering when Commits Don't Exist (or only exist locally)

If the commits don't exist, or they exist but haven't been pushed (e.g. you chose a release branch name that conflicted
with an existing branch) then you can just clean up locally.  Where there are no commits at all you can do a reset i.e.

```bash
$ git reset --hard HEAD
```

If there are some commits present then you need to identify the hash of the commit prior to the Maven release commits
(the ones that have `[maven-release-plugin]` in their log lines) and reset specifically to that hash e.g.

```bash
$ git reset --hard <hash>
```

You should also check whether you need to [Delete the Tag](#recovering-when-the-tag-was-created).  Once this is done you
should have a clean release branch without any Maven release changes where you can address any underlying issues on your
branch before attempting the [Maven Release Preparation](#maven-release-preparation) again.

#### Recovering when the Tag was Created

Note that if Maven release got as far as pushing the git tag to the remote repository you will need to delete the tag
both locally and on GitHub if you want to retry a release with that same tag:

```bash
$ git tag --delete <version>
$ git push --delete origin <version>
```

### Pushing the Prepared Release

Make sure you have pushed your release preparation to the remote repository, note that the Maven release plugin
**SHOULD** do this automatically for you **BUT** it's worth double-checking as otherwise subsequent steps will fail:

```bash
$ git push
$ git push --tags
```

### Performing the Release

To perform the actual Maven release:


```bash
$ git checkout -B release/1.2.3
$ mvn release:clean release:perform
```

If this fails (which should be rare if you've got this far) then refer to [Recovering from a Release
Failure](#recovering-from-a-release-failure).

Once this completes successfully the Maven artifacts will all have been pushed up to the relevant registries.

### Recovering from a Release Failure

Failures here should be rare assuming that release preparation was successful.  Recovering from a failure here is
somewhat more difficult. If a release fails you will need to reset your release branch accordingly:

Remove the 2 commits created by the Maven release plugin: 
```bash
$ git reset --hard HEAD~2
```
Delete the created tag locally: 
```bash
$ git tag --delete <tag>
```
Delete the created tag remotely: 
```bash
$ git push --delete origin <tag>
```

You can then make additional commits on your release branch to address the reasons for the failure and force push (`git push -f`) the revised branch as needed.

Once the commits and tags have been deleted you can attempt to [Perform the
Release](#performing-the-release) again.

### Releasing the Docker Image

There is no developer interaction needed to release the Docker Image, a GitHub Actions build will automatically be
kicked off for the release tag, e.g. https://github.com/Telicent-oss/smart-cache-entity-resolution/actions/runs/6744176903, and 
this will automatically publish the images with the given tag when that build completes.

Occasionally, one of the build steps might fail due to a transient error in which case you can rerun the failed jobs
from the GitHub Actions interface.

### Merging the Release Branch back to Main

Finally, please go to GitHub and open a PR to merge the release branch you created back into `main`, once approved and
merged **DO NOT** delete the release branch (branch protection rules will actually prevent you from doing this).
Release branches **MUST** be preserved for future reference.

## Developing

See [Developing documentation](docs/developing.md) for more detailed guidance on developer conventions.

## Tests

The build includes a variety of unit and integration tests and enforces that minimum code coverage levels are
achieved on key modules. Some integration tests require being able to start up Docker containers to provide
test services for the tests to execute against. If your environment does not have Docker installed/running then
these tests will fail. You can deactivate the relevant tests by adding `-P-docker` to deactivate the `docker`
profile that runs these tests by default e.g.

```bash
$ mvn clean install -P-docker
```

## Version of Docker image for Elasticsearch

This project requires a custom version of the Docker image for Elasticsearch which includes the [Telicent plugin for Elasticsearch](https://github.com/Telicent-io/telicent-elastic).

This image is on [Dockerhub](https://hub.docker.com/repository/docker/telicent/elasticsearch/general). This is required
for running the tests.

The tests will try to pull down this image which can be large. That can fail due to timeouts. 
It's better to pull down the image ahead of time. 
```bash
$ docker pull telicent/elasticsearch:8.12.2.1
```

### Using an External ElasticSearch instance for testing

As noted above various integration tests need to have an ElasticSearch instance available which is provided using a
Docker container by default. However on some developers systems the startup/teardown times for these containers can
be extremely high to the detriment of developer productivity. For some (but not all) tests it is possible to force the
use of an external ElasticSearch instance as follows.

Firstly start up an external instance:

```bash
$ docker run -p 19200:9200 -e discovery.type=single-node telicent/elasticsearch:7.17.5.1
```

Then in a separate terminal:

```bash
$ mvn verify -pl :entity-resolution-api-server -Delastic.external=19200
```

Note that the port given here **MUST** match the port used in your Docker command and **SHOULD** be `19200` to match 
the test port coded in the relevant tests.  If you need to use an alternative port for any reason then you can also 
set the `elastic.port` property to do so e.g.

```bash
$ mvn verify -pl :entity-resolution-api-server -Delastic.external=12345 -Delastic.port=12345
```

## Building the Docker Container

If you want to build the Docker containers for this project you can do so:

```bash
$ ./docker-build.sh
```

The build produces multiple images:

- `smart-cache-elastic-can-index:<tag>` - The Canonical Index Command Line image.
- `smart-cache-entity-resolution-api:<tag>` - The REST API Server image.

Where `<tag>` is the detected Git branch for the repository, you can specify a custom `<tag>` as the first argument to
this script e.g.

```bash
$ ./docker-build.sh my-cool-feature
```

Would produce images named like `smart-cache-elastic-can-index:my-cool-feature`.

Additionally, if you provide a Docker repository as the second argument to the script the image names will be prefixed
with that registry and the application images pushed to it on a successful build e.g.

```bash
$ ./docker-build.sh my-cool-feature some-registry.com/telicent
```

Would produce images named like `some-registry.com/telicent/smart-cache-elastic-can-index:my-cool-feature` and push that
image up to the registry.

Finally, if your current git branch is `main` the non-base images will also be re-tagged as `latest` and the `latest`
tag pushed as well.
