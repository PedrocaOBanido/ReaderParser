# Journey tests

Journey tests are XML-specified behavioral tests that run against the app on a
real Android emulator. They complement Compose UI unit tests by verifying
screen-to-screen flows, navigation, and runtime behavior that unit tests
cannot cover.

## Format

```xml
<journey name="Descriptive Name">
   <description>
      What this journey verifies and under what conditions.
   </description>
   <actions>
      <action>
         Natural-language description of a UI interaction.
      </action>
      <action>
         Verify that some expected state is visible on screen.
      </action>
   </actions>
</journey>
```

## Execution

Journeys are **agent-driven**. A script cannot interpret natural-language
actions. Instead:

1. **Pipeline**: `scripts/emulator` provisions the AVD, `scripts/run-journeys --setup`
   installs the APK and prints the journey steps.
2. **Agent**: loads `skill("android-cli")`, reads the journey XML, and executes
   each `<action>` using:
   - `android layout` — inspect the UI tree as JSON
   - `android layout --diff` — check what changed since last layout call
   - `android screen capture` — visual inspection
   - `android screen capture --annotate` — labeled screenshot for element targeting
   - `android screen resolve --screen <file> --string "#N"` — convert label to coordinates
   - `adb shell input tap/swipe/keyevent` — perform interactions

3. **Result**: agent outputs JSON per the format in
   `.opencode/skills/android-cli/references/journeys.md` §Summarizing.

## Naming conventions

- One file per screen/flow: `library.xml`, `browse.xml`, `series.xml`
- Verifications begin with "Verify" or "Check"
- Interactions describe the exact UI element ("Tap the search icon", not "Search")

## CI

Journey tests run via the `journey.yml` workflow (manual trigger only:
`workflow_dispatch`). The workflow provisions an emulator, assembles the debug
APK, and prepares the environment. The agent then executes journeys against the
running app.

## Adding a new journey

1. Create `journeys/<screen>.xml` using the format above.
2. Test locally: `scripts/run-journeys journeys/<screen>.xml`
3. Have an agent execute it against a running emulator.
4. Iterate on the steps until they pass consistently.

## Relationship to Compose tests

| Layer | What it tests | Pattern |
|---|---|---|
| Compose UI test | Individual composable rendering + interaction | Given state, assert UI elements present |
| Journey test | Screen-to-screen flow + runtime behavior | Launch app → interact → verify screens |

Journeys do not replace Compose tests. They cover what unit/mock tests cannot:
real device rendering, navigation transitions, and system interactions.
