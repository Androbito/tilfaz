---
name: figma-to-native-ui
description: Translate Figma frames, components and design tokens into production-quality native UI: Jetpack Compose for Android and SwiftUI for iOS, reusing the project's design system instead of hardcoding values. Use this skill whenever the user shares a Figma link or node id, selects a frame in Figma Dev Mode, says "implement this design", "build this screen from Figma", "convert this mockup to Compose/SwiftUI", wants design tokens (colours, typography, spacing) synced from Figma variables, or attaches a screenshot of a design to implement natively.
---

# Figma to Native UI

The Figma MCP server's code output defaults to React + Tailwind. That is a useful description of the design, not mobile code. This skill turns Figma data into idiomatic Compose / SwiftUI that plugs into the project's existing theme, components and architecture.

## Inputs, in order of preference

1. **Figma MCP server** (official, desktop or remote). Tools used here: `get_metadata`, `get_design_context`, `get_variable_defs`, `get_screenshot`, `download_assets`, `get_code_connect_map`, `search_design_system`. Tool names evolve: list the server's tools first and use the closest equivalents.
2. **Figma REST API** with a personal access token the user provides via an environment variable (`FIGMA_TOKEN`): `GET https://api.figma.com/v1/files/{fileKey}/nodes?ids={nodeId}` for structure, `GET /v1/images/{fileKey}?ids=...&format=svg|png` for assets, `GET /v1/files/{fileKey}/variables/local` for variables (Enterprise). Never ask the user to paste the token into chat; read it from the environment.
3. **Screenshot only.** Works, but spacing and colours are estimates. Say so, and map every estimated value to the nearest existing design token rather than inventing new values.

A Figma URL looks like `https://www.figma.com/design/<fileKey>/<name>?node-id=<a-b>`. The node id in API calls uses `a:b` (replace `-` with `:`).

## Workflow

1. **Read the project first.** Find the theme (`MaterialTheme`/custom `AppTheme`, `Color.kt`, `Type.kt`, SwiftUI `Color`/`Font` extensions, asset catalogs) and the component library (`:core:designsystem`, `DesignSystem` package). List available components and tokens. This is what the generated code must use.
2. **Get the structure** with `get_metadata` for large frames (cheap outline), then `get_design_context` on the relevant nodes. For big screens, go section by section (header, list item, footer) instead of one huge request.
3. **Get the tokens** with `get_variable_defs`. Map each Figma variable to an existing code token by name and value. Report unmapped variables; propose adding them to the theme rather than inlining hex values.
4. **Get a screenshot** with `get_screenshot` to see the intended result and to compare at the end.
5. **Check Code Connect** (`get_code_connect_map`) and `search_design_system`: if a Figma component maps to a code component, use the code component. Otherwise check the project's design system for an equivalent by name and shape before creating anything new.
6. **Translate** using the mapping rules in `references/mapping.md`. Produce:
   - A **stateless** screen/component that takes a UI state and event callbacks (fits the `mobile-clean-architecture` skill if installed).
   - Previews: `@Preview` (light, dark, large font, RTL) / `#Preview` with the same variants.
   - New reusable components in the design system module when a pattern appears more than once.
7. **Assets.** Download icons as SVG and convert to Android Vector Drawables or add to the iOS asset catalog as PDF/SVG with "Preserve Vector Data". Prefer an existing icon set (Material Symbols, SF Symbols) when the design uses a standard icon.
8. **Verify.** Build. Render previews or snapshot tests and compare with the Figma screenshot side by side if tools allow (Paparazzi/Roborazzi, swift-snapshot-testing). List any deviations and why.

## Rules

- **Never copy absolute positions.** Figma absolute x/y become layout containers: auto layout -> `Row`/`Column`/`HStack`/`VStack`, constraints -> alignment and weights.
- **Never hardcode** colours, font sizes or spacing when a token exists. Hardcoded values are acceptable only for genuinely one-off values, with a comment.
- **Design for real content**: text can be longer, translated, or in Arabic (RTL); lists can be empty or long. Use `weight`, `maxLines` + overflow, `fixedSize`, `lineLimit` deliberately.
- **Accessibility comes with the translation**: content descriptions / accessibility labels for icons and images, 48dp / 44pt minimum touch targets even if the visual is smaller, semantic headings, Dynamic Type / font scaling respected.
- **Interaction states** (pressed, disabled, focused, loading, error) are often separate Figma variants; ask for or look for them, and implement them.
- **Platform conventions win** over pixel-perfection when they conflict (navigation bars, sheets, back behaviour, system fonts). Flag the deviation to the user.
- If the design is ambiguous (missing state, unclear behaviour), implement the sensible default and list the open questions at the end.

## Output summary for the user

End with: files created/changed, tokens mapped vs missing, components reused vs created, assets added, and open questions for the designer.
