---
name: Architectural Noir
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#3a3939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#d4c4b7'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#9d8e83'
  outline-variant: '#50453b'
  surface-tint: '#f1bc8b'
  primary: '#f1bc8b'
  on-primary: '#492904'
  primary-container: '#b6875a'
  on-primary-container: '#412200'
  inverse-primary: '#7e562d'
  secondary: '#e9c176'
  on-secondary: '#412d00'
  secondary-container: '#604403'
  on-secondary-container: '#dab36a'
  tertiary: '#f4bb92'
  on-tertiary: '#4a280a'
  tertiary-container: '#b98661'
  on-tertiary-container: '#422105'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdcbe'
  primary-fixed-dim: '#f1bc8b'
  on-primary-fixed: '#2d1600'
  on-primary-fixed-variant: '#633f18'
  secondary-fixed: '#ffdea5'
  secondary-fixed-dim: '#e9c176'
  on-secondary-fixed: '#261900'
  on-secondary-fixed-variant: '#5d4201'
  tertiary-fixed: '#ffdcc5'
  tertiary-fixed-dim: '#f4bb92'
  on-tertiary-fixed: '#301400'
  on-tertiary-fixed-variant: '#653d1e'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Playfair Display
    fontSize: 72px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Playfair Display
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
  headline-md:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1'
    letterSpacing: 0.1em
spacing:
  unit: 4px
  gutter: 32px
  margin-desktop: 64px
  margin-mobile: 20px
  panel-gap: 1px
---

## Brand & Style

This design system establishes an elite atmosphere for competitive programming, moving away from gamified neon aesthetics toward a sophisticated, cinematic command center. The visual narrative is built on the concept of an "Architectural Arena"—where code is treated as high-stakes literature and the interface acts as a quiet, premium canvas for intellectual combat.

The style is a blend of **Minimalism** and **High-End Editorial**. It utilizes vast negative space, hairline dividers, and a strict adherence to grid systems to evoke a sense of precision and prestige. The emotional response should be one of focused intensity, quiet confidence, and academic excellence. Every element is intentionally "un-decorated," relying on structural integrity and refined typography rather than decorative effects.

## Colors

The palette is anchored in **Obsidian (#0A0A0A)**, creating a deep, void-like backdrop that eliminates distractions. Functional depth is achieved through a hierarchy of monochromatic blacks rather than elevation shadows. 

Accents are strictly reserved for **Aged Bronze** and **Antique Gold**, used sparingly to highlight critical actions, active states, or elite achievements. Status colors are intentionally desaturated (muted jade, dusty crimson, red clay) to maintain the editorial sobriety of the platform, ensuring that "Accepted" or "Wrong Answer" notifications feel like grave reports rather than arcade alerts.

## Typography

Typography is the primary vehicle for the "Editorial" feel. **Playfair Display** provides a sophisticated, literary contrast to the technical nature of the content, used exclusively for large headings and display moments. 

**Geist** serves as the functional workhorse for UI text, offering a clean, technical sans-serif that remains legible at all sizes. **JetBrains Mono** is utilized for code blocks and meta-labels, reinforcing the precision of the platform.

- **Asymmetric Contrast**: Pair large Serif display text with small, monospaced uppercase labels to create a high-fashion, technical layout.
- **Hierarchy**: Use generous line heights for body text to ensure long problem descriptions remain readable and authoritative.

## Layout & Spacing

The layout philosophy follows a **Strict Fixed Grid** (12-columns) with intentional asymmetry. Wide margins and large gutters (32px) are essential to prevent the interface from feeling cluttered.

- **Cinematic Panels**: Instead of traditional cards, use full-height panels or edge-to-edge containers separated by 1px borders (the "panel-gap").
- **Asymmetry**: Important content (like the code editor) should command 8 columns, while meta-information and timers occupy 4 columns, often offset to create a dynamic, editorial flow.
- **Breakpoints**: 
    - Desktop (1440px+): 12 columns, 64px margins.
    - Tablet (768px): 8 columns, 32px margins.
    - Mobile (375px): 4 columns, 20px margins, stacking panels vertically.

## Elevation & Depth

This design system avoids drop shadows entirely. Depth is communicated through **Tonal Layering** and **Hairline Outlines**.

- **Hairline Borders**: Elements are defined by 1px borders in `Charcoal Slate` or low-opacity white (10%).
- **Tonal Stepping**: The base background is `Obsidian`. Interactive or focused containers use `Carbon Black`. Overlays and modals use `Deep Graphite`.
- **Bronze Accents**: Depth is highlighted not by shadow, but by a 2px top-border or side-border in `Aged Bronze` to signify active states.
- **Backdrop Blurs**: When modals are necessary, use a heavy 20px backdrop blur with a 60% opacity `Obsidian` fill to maintain cinematic immersion.

## Shapes

The shape language is strictly **Sharp (0px)**. Rounded corners are prohibited to maintain the architectural, elite feel of a command center. 

All buttons, input fields, containers, and code blocks must have hard 90-degree angles. This geometric rigidity communicates seriousness, precision, and a high-barrier-to-entry aesthetic.

## Components

### Buttons
- **Primary**: Sharp edges, transparent background, 1px `Aged Bronze` border. On hover: Solid `Aged Bronze` background with `Obsidian` text.
- **Ghost**: No border, `JetBrains Mono` caps text. On hover: Underline in `Antique Gold`.

### Terminal Inputs
- Fields consist of a 1px bottom-border only, or a full 1px box in `Charcoal Slate`.
- Focus state: Border color changes to `Antique Gold` with no glow.
- Cursor: A solid block character (█) in the `Primary` color for the coding environment.

### Status Indicators
- **Problem Lists**: Status is shown via a small vertical bar (2px wide) on the left of the list item, colored by the status palette (Jade for AC, etc.).
- **Typography-led**: Status is primarily conveyed through text labels using the `label-caps` style.

### Sophisticated Timers
- Large `Playfair Display` numerals for hours/minutes.
- Sub-text in `JetBrains Mono` for seconds.
- Enclosed in a 1px bronze frame for high-stakes contests.

### Lists & Tables
- No row stripping. Use 1px `Charcoal Slate` dividers between rows.
- High horizontal padding (24px) for a luxurious, airy feel.