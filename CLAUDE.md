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

### Dashboard
- **Base URL**: https://jenkins.build.msap.io/job/application/job/AMF/job/amf-core
- **Branch URL format**: `{base-url}/job/{branch-name}` (URL encode `/` as `%252F`)
- **Example**: https://jenkins.build.msap.io/job/application/job/AMF/job/amf-core/job/release%252F5.10.1

### Stages to Modify
- **Publishing stages**: `Publish`

### Jenkinsfile Updates
- **For RC**: Add `"release/*"` to branch list in `Publish` stage
- **For Release**: Remove `"release/*"` from branch list in `Publish` stage

## Dependencies Impact
After releasing amf-core, update the version in:
- amf-aml (next in release line)
