# Figma to Compose / SwiftUI mapping

## Layout

| Figma | Jetpack Compose | SwiftUI |
| --- | --- | --- |
| Auto layout, vertical | `Column(verticalArrangement = Arrangement.spacedBy(gap))` | `VStack(spacing: gap)` |
| Auto layout, horizontal | `Row(horizontalArrangement = Arrangement.spacedBy(gap))` | `HStack(spacing: gap)` |
| Auto layout, wrap | `FlowRow` | custom `Layout` or `LazyVGrid` with adaptive columns |
| Padding (auto layout) | `Modifier.padding(...)` | `.padding(...)` |
| Space between | `Arrangement.SpaceBetween` | `Spacer()` between items |
| Fill container | `Modifier.weight(1f)` / `fillMaxWidth()` | `.frame(maxWidth: .infinity)` |
| Hug contents | default (wrap content) | default |
| Fixed width/height | `Modifier.size()/width()/height()` | `.frame(width:height:)` |
| Min/max width | `widthIn(min, max)` | `.frame(minWidth:maxWidth:)` |
| Absolute position inside frame | `Box` + `Modifier.align()` / `offset()` | `ZStack(alignment:)` / `.overlay(alignment:)` |
| Scrollable frame | `LazyColumn` (lists) / `verticalScroll` (short content) | `List` / `ScrollView` + `LazyVStack` |
| Grid | `LazyVerticalGrid` | `LazyVGrid` |
| Clip content | `Modifier.clip(shape)` | `.clipShape(...)` |
| Constraints left+right / scale | `fillMaxWidth()` | `.frame(maxWidth: .infinity)` |

Modifier order in Compose matters: padding before background = outer margin; background before padding = inner padding.

## Styling

| Figma | Compose | SwiftUI |
| --- | --- | --- |
| Fill colour variable `color/primary` | `MaterialTheme.colorScheme.primary` or `AppTheme.colors.primary` | `Color.primary` asset / `Color.theme.primary` |
| Corner radius | `RoundedCornerShape(r)` from theme shapes | `RoundedRectangle(cornerRadius: r)` |
| Stroke | `Modifier.border(width, color, shape)` | `.overlay(RoundedRectangle(...).stroke(color, lineWidth: w))` |
| Drop shadow | `Modifier.shadow(elevation, shape)` (approximation) | `.shadow(color:radius:x:y:)` |
| Opacity | `Modifier.alpha()` | `.opacity()` |
| Linear gradient | `Brush.linearGradient` | `LinearGradient` |
| Blur | `Modifier.blur()` (API 31+) | `.blur(radius:)` / `.background(.ultraThinMaterial)` |
| Text style `Heading/H1` | `MaterialTheme.typography.headlineLarge` | `.font(.theme.h1)` or `.font(.largeTitle)` |
| Letter spacing (%) | `letterSpacing = (percent / 100).em` | `.tracking(fontSize * percent / 100)` |
| Line height | `lineHeight` in `TextStyle` | `.lineSpacing(lineHeight - fontLineHeight)` |
| Text truncation | `maxLines` + `TextOverflow.Ellipsis` | `.lineLimit(n)` + `.truncationMode` |

## Units

Figma px at 1x = dp on Android = pt on iOS. Font px = sp on Android (keep sp so font scaling works) and should map to a Dynamic Type text style on iOS rather than a fixed size.

## Components

| Figma | Compose | SwiftUI |
| --- | --- | --- |
| Component with variants (`state=pressed/disabled`) | One composable with parameters / enum, interaction source for pressed | One `View` with parameters, `ButtonStyle` for pressed |
| Instance with overrides | Composable call with arguments | View init with arguments |
| Boolean property | `Boolean` parameter | `Bool` parameter |
| Instance swap (icon) | `@Composable () -> Unit` slot or `ImageVector` param | `@ViewBuilder` content or `Image` param |
| Text property | `String` parameter (from string resource at call site) | `LocalizedStringKey` parameter |

## Icons and images

- Standard icons: Material Symbols (`Icons.*`) / SF Symbols (`Image(systemName:)`).
- Custom icons: SVG -> Vector Drawable (Android Studio import or `vd-tool`), SVG/PDF in Assets.xcassets with Preserve Vector Data.
- Directional icons (arrows, chevrons): mirror in RTL (`autoMirrored = true`, `.flipsForRightToLeftLayoutDirection(true)`).
- Bitmaps: export @2x/@3x for iOS, WebP density buckets or a single high-res asset with Coil for Android.

## Tokens file generation

When `get_variable_defs` returns many variables and the project has no theme yet, generate:

- Android: `core/designsystem/theme/Color.kt`, `Type.kt`, `Spacing.kt` (a `Spacing` object or `CompositionLocal`), and a `Theme.kt` exposing them with light and dark schemes.
- iOS: `DesignSystem/Colors.swift` (colours from the asset catalog with light/dark appearances), `Typography.swift`, `Spacing.swift` (an `enum Spacing { static let s: CGFloat = 8 }`).

Keep token names identical to Figma variable names (camelCased) so designers and developers speak the same language.
