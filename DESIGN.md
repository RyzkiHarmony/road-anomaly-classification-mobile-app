---
name: Verdant Clarity
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#404943'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#707973'
  outline-variant: '#bfc9c1'
  surface-tint: '#2c694e'
  primary: '#0f5238'
  on-primary: '#ffffff'
  primary-container: '#2d6a4f'
  on-primary-container: '#a8e7c5'
  inverse-primary: '#95d4b3'
  secondary: '#3e6750'
  on-secondary: '#ffffff'
  secondary-container: '#bdeacd'
  on-secondary-container: '#426b54'
  tertiary: '#005236'
  on-tertiary: '#ffffff'
  tertiary-container: '#116c4a'
  on-tertiary-container: '#98eabf'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#b1f0ce'
  primary-fixed-dim: '#95d4b3'
  on-primary-fixed: '#002114'
  on-primary-fixed-variant: '#0e5138'
  secondary-fixed: '#c0edd0'
  secondary-fixed-dim: '#a4d1b4'
  on-secondary-fixed: '#002112'
  on-secondary-fixed-variant: '#264f39'
  tertiary-fixed: '#a1f4c8'
  tertiary-fixed-dim: '#86d7ad'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-metric:
    fontFamily: Sora
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Sora
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Sora
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Sora
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Sora
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Sora
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Sora
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 40px
  xl: 64px
  gutter: 20px
  margin: 32px
---

## Brand & Style
The design system is engineered for a road anomaly detection platform that balances high-tech precision with an approachable, calming aesthetic. The brand personality is **reliable, refreshing, and professional**, aimed at infrastructure managers and city planners who require split-second data visualization without the stress of "alarmist" UI.

The style is **Modern/Tactile**, utilizing a light mode palette that evokes a sense of environmental stewardship and clarity. It prioritizes high legibility and a softened "technological" feel—moving away from the cold, dark-mode aesthetics of traditional SaaS toward a more open, breathable, and human-centric interface. Elements are grounded by organic roundedness and deep, natural greens to foster trust.

## Colors
The palette is built on a foundation of **Forest Green (#2D6A4F)**, which serves as the primary action color, signifying growth and systemic health. This is contrasted with **Mint Green (#B7E4C7)**, used for subtle backgrounds, positive state indicators, and soft highlights to reduce visual fatigue.

**Surface Layers:**
- **Primary Surface:** #F8F9FA (Off-white) for the main application background to maintain a "clean" and "airy" environment.
- **Container Backgrounds:** #E9ECEF (Soft Blue-Grey) for secondary regions like sidebars, card backgrounds, and inset groupings to provide gentle structural separation without harsh borders.
- **Text:** Dark Forest Green tints are used for primary text to maintain a cohesive organic feel, while neutral greys are reserved for secondary metadata.

## Typography
This design system utilizes **Sora** exclusively to leverage its geometric clarity and modern technical spirit. 

**Hierarchy Strategy:**
- **Metrics:** Use `display-metric` (Bold, 700) for high-impact data points like "Anomaly Count" or "Safety Rating."
- **Labels:** Use Medium (500) or SemiBold (600) weights for all UI labels and button text to ensure they remain distinct from body copy.
- **Readability:** Body text is set with generous line heights to ensure long-form reports or log descriptions are easy to parse during high-intensity monitoring.

## Layout & Spacing
The layout follows a **fluid grid system** for data-heavy dashboard views and a **contained fixed-width model** for settings and reporting pages. 

- **Grid:** A 12-column layout on desktop with 20px gutters. 
- **Margins:** A wide 32px safe-area margin on desktop, scaling down to 16px on mobile to maximize screen real estate.
- **Rhythm:** Spacing is strictly based on an 8px scale. Use `md` (24px) for padding within cards and `lg` (40px) for section-level separation to maintain the "vibrant yet calm" sense of space.

## Elevation & Depth
To achieve the "calm" feel requested, this design system avoids heavy shadows or high-contrast borders. Instead, it utilizes **Tonal Layering** combined with **Ambient Soft Shadows**.

- **Level 0 (Base):** #F8F9FA surface.
- **Level 1 (Cards/Containers):** #FFFFFF surface with a very subtle 1px border (#E9ECEF) or a soft, diffused shadow (0px 4px 20px rgba(45, 106, 79, 0.05)).
- **Level 2 (Active/Floating):** Use a slightly more pronounced shadow (0px 8px 30px rgba(45, 106, 79, 0.08)) for modals or active selection cards.
- **Interaction:** Hover states should not lift significantly but rather shift in background color (e.g., from White to Mint Green tint).

## Shapes
The shape language is consistently **Rounded**, using a 16px (`rounded-lg`) corner radius as the standard for all primary containers and cards. This eliminates "sharpness" and contributes to the approachable, reliable personality of the app.

- **Buttons:** 12px to 16px radius, depending on scale.
- **Input Fields:** 12px radius to maintain a professional, slightly more structured look than the cards.
- **Progress Bars:** Fully pill-shaped (rounded-full) for a smooth, technical feel.

## Components
- **Buttons:** Primary buttons use Forest Green (#2D6A4F) with white text. Secondary buttons use a Mint Green (#B7E4C7) background with Forest Green text. All buttons should have a minimum height of 44px for accessibility.
- **Status Chips:** Use Mint Green for "Healthy/Resolved" states. Use a soft muted amber for "Detected" and a soft coral for "Critical," but keep the saturation low to maintain the "calm" atmosphere.
- **Cards:** White backgrounds, 16px rounded corners, and no heavy borders. Use the soft blue-grey (#E9ECEF) for header sections within cards.
- **Input Fields:** Filled style using the #E9ECEF background with a Forest Green 2px bottom stroke or border focus state.
- **Anomaly Indicators:** Use a pulsing Forest Green dot for active monitoring states to provide a "heartbeat" feel to the live data.
- **Data Visualization:** Graphs should use varying tints of the Primary and Secondary greens, with the soft blue-grey used for grid lines.