# jbossws-common

Project contains common classes usind in jbossws-cxf and across multiple jbossws sub-projects.

Releasing
-----------

### Prerequisites

* Check Resources availability

  Check if there is SNAPSHOT version dependency and upgrade to the release version before the tag/release.

* Contents checks

  Check the [JBWS JIRA](https://issues.redhat.com/projects/JBWS/) to make sure all the must-have features or issues are included
* Quality / testing gate
    - Make sure the CI is passed/green as expected.
    - Check if other components have some major CVE and it needs an upgrade
* PR queue
    - Review the PR queue and check all desired contributions are included
* Branch preparation
    - Branch the codebase in preparation for the release if necessary
* JDK and Maven to build the release
    - Check the JDK version to run the build is the latest 11
    - Maven is the latest version

### Source Tagging
`maven-release-plugin` is the maven plugin we used to tag and change the development version:

``` mvn release:prepare -DskipTests=true```

This is interactive command, and make sure you input the correct release version number and next

### Publish Artifacts
Uploading artifacts with the following commands:

```git checkout new-tag-version```

```mvn deploy```

After the artifacts are all uploaded, go to jboss nexus to publish these artifacts.
