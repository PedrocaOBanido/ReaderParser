# app/src/

## Responsibility

Standard Android source set root. Splits the module's source code into three conventional directories: `main/` (production), `test/` (JVM unit tests), and `androidTest/` (instrumented tests including Room migration tests and Compose UI tests).

## Design

- **Three-source-set layout** mirroring the Gradle Android plugin convention.
- `test/` executes on the JVM via `testDebugUnitTest` — covers ViewModels, repositories, sources, and mappers.
- `androidTest/` executes on a device/emulator — covers Room migrations, WorkManager workers, and Compose UI integration tests.
- Room schema JSON files are symlinked into `androidTest/assets/` so `MigrationTestHelper` can reference them (`sourceSets.getByName("androidTest").assets.srcDirs("$projectDir/schemas")`).

## Flow

- During builds, `main/` is always compiled. `test/` and `androidTest/` are compiled only when their respective Gradle tasks are invoked.
- `test/` sources see the same classpath as `main/` plus test libraries (JUnit, Turbine, MockEngine, Truth).
- `androidTest/` sources see the full main classpath plus instrumented libraries (Room testing, WorkManager testing, Compose UI test).

## Integration

- `test/` sources import directly from `main/` classes — no separate test module.
- `androidTest/assets/` is auto-configured to resolve `$projectDir/schemas` for Room migration verification.
- Both test source sets share the naming convention: `<ProductionClass>Test` for JVM, `<ProductionClass>AndroidTest` for instrumented.
