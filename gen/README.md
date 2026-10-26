# gen/ — generated code, committed

Everything here is generated from [`proto/`](../proto) by `make proto` (see
[`buf.gen.yaml`](../buf.gen.yaml)). **Never edit it by hand**: change the `.proto`, run
`make proto`, and commit both. CI fails if `gen/` doesn't match the contracts.

| Folder | Used by | Package |
|---|---|---|
| `java/` | the Java services (added as a source folder by `build-helper-maven-plugin`) | `com.friendlyfire.proto.friendlyfire.<area>.v1` |
| `python/` | the Python services (editable path dependency) | `friendlyfire-proto`, imported as `friendlyfire.<area>.v1` |
| `go/` | match-data-service (`replace` in its `go.mod`, and `go.work`) | `github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/<area>/v1` |

The only hand-maintained files are the package metadata, which `make proto` keeps:
`python/pyproject.toml` and `go/go.mod` / `go/go.sum`. Their runtime versions must match
the Buf plugin versions in `buf.gen.yaml`.
