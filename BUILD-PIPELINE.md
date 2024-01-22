# build.yaml
Use case: Pull -request 
use version plugin to avoid conflicts between integration branch and PRs

Result: deploy to Maven repo or Docker registry

Documentation: avoid deploy for draft PRs

# release.yaml

## Create Release
1. set release version and tag git repo, specify release version 
2. build project, but dont't deploy
2. merge to release branch (main/master)
3. build project in release branch and deploy to Maven repo or Docker registry


## Create Hotfix Branch

1. create branch 
2. set release version and tag git repo, specify release version
2. build project, but dont't deploy
2. merge to release branch
3. build project in release branch and deploy to Maven repo or Docker registry
