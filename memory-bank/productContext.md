# Product context

## Main flows

- Library: saved series and local reading/download state.
- Browse: source-backed discovery via popular, latest, and search flows.
- Series: metadata plus chapter list for a selected title.
- Reader: separate novel and manhwa experiences driven by
  `ChapterContent.Text` versus `ChapterContent.Pages`.
- Downloads and Settings: queue visibility plus app behavior preferences.

## Source model

- Each site is implemented as a `Source` plugin.
- Repositories translate between source data, local persistence, and
  presentation.
- The UI above repositories should not care which concrete site provided the
  content.

## UX expectations

- `*Screen` wires ViewModel state/effects and navigation.
- `*Content` stays stateless for previews and Compose tests.
- Errors surface at the ViewModel/UI boundary instead of being hidden inside
  sources or repositories.
