# amf-core

**Repository**: https://github.com/aml-org/amf-core

## Release Process
Follows the regular release process (see ~/mulesoft/CLAUDE.md).

## Release Position
- **First** in the RC/release line
- RC and full version must be adopted in **amf-aml** before its release

## Files & Locations

### Version File
- `build.sbt`

### Dependencies
- syaml (check for new version before publishing)

## Jenkins Configuration

### Stages to Modify
- **Publishing stages**: `Publish`

### Jenkinsfile Updates
- **For RC**: Add `"release/*"` to branch list in `Publish` stage
- **For Release**: Remove `"release/*"` from branch list in `Publish` stage

## Release Steps

### Publishing RC (x.y.z-RC.r)
1. Check if syaml has a new version and update if needed
2. From develop: `git checkout -b release/x.y.z`
3. Edit Jenkinsfile: add `"release/*"` to Publish stage branches
4. Edit build.sbt: update version to `x.y.z-RC.r`
5. Commit: `git commit -m "Publish x.y.z-RC.r"`
6. Push: `git push -u origin release/x.y.z`

### Publishing Release (x.y.z)
1. Follow standard three-PR process (setup → master → develop)
2. After merging to master, manually tag: `git tag x.y.z origin/master && git push origin x.y.z`

### Publishing Hotfix (x.y.z-n)
1. Checkout/create `support/x.y.z` branch from tag
2. Cherry-pick required commits
3. Update version in build.sbt to `x.y.z-n`
4. Ensure Jenkinsfile has `support/*` in Publish stage (for first HF only)
5. Push and create PR to merge into `support/x.y.z`

## Dependencies Impact
After releasing amf-core, update the version in:
- amf-aml (next in release line)
