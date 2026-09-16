---
name: website-cloner
description: Clones a target URL's visual layout, design system, typography, animations, and components into production-ready code.
triggers:
  - clone website
  - replicate website
  - copy landing page
  - clone framer site
  - replicate design from URL
---

# Website Cloner & Aesthetic Reconstructor Skill

## Objective
Reconstruct any target website or landing page into a pixel-perfect, modern, high-performance, and responsive implementation without relying on generic placeholders or sloppy approximations.

---

## 5-Stage Cloning Methodology

### 1. Deep Reconnaissance & Asset Extraction
- **DOM & Structure Extraction**: Fetch and parse the target page's semantic HTML hierarchy (nav, hero, feature bentos, stats, tabs, testimonials, FAQ accordions, footers).
- **Design Token Discovery**:
  - Exact background tones (canvas, elevated cards, popovers, glassmorphism overlays).
  - Accent colors, borders (`border-white/10`, `border-zinc-800`), shadows, and radius scales (`rounded-2xl`, `rounded-full`).
  - Typography stack: exact font pairings (e.g., Geist, Inter, Helvetica Now, Outfit, Plus Jakarta Sans), weights, tracking (letter-spacing), and line heights.
- **Media & Asset Harvesting**: Identify SVG vector icons, phone mockup geometry, badge logos, and high-res media. Recreate or self-contain vector assets cleanly.

### 2. Design System Architecture
- Establish CSS custom properties or design tokens:
  ```css
  :root {
    --bg-canvas: #0f0f0f;
    --bg-surface: #1c1c1c;
    --bg-surface-elevated: #242424;
    --border-subtle: rgba(255, 255, 255, 0.08);
    --text-primary: #ffffff;
    --text-secondary: #a3a3a3;
    --text-muted: #737373;
    --accent-sand: #e1d6c8;
    --radius-pill: 9999px;
    --radius-card: 24px;
    --font-heading: 'Geist', 'Inter Display', sans-serif;
    --font-body: 'Inter', sans-serif;
  }
  ```

### 3. Component Hierarchy & Modularization
Divide the replica into modular, cleanly separated components:
- `Navbar`: Sticky blurred backdrop, responsive mobile menu drawer, CTA pills.
- `HeroSection`: Headline with tight tracking, subtitle, App Store / Play Store download triggers, interactive live Phone Mockup showing the text-based launcher interface.
- `StatsBento`: Numerical highlights (`93%`, `2hrs+ daily`) with label hierarchy and subtle border highlights.
- `ComparisonSection`: Interactive Before/After visual comparison (chaotic colorful icons vs peaceful minimalist text list).
- `FeatureGrid`: Bento cards for App Blocker, Mindful Launch Delay, In-App Reminders, Blocking Schedules, and Greyscale Mode.
- `TestimonialsWall`: Multi-column masonry or slider grid with verified user badges and star ratings.
- `FaqAccordion`: Accessible expand/collapse accordion with smooth chevron rotation and height transitions.
- `FinalCta`: Full-width dark gradient card with direct app download CTAs.
- `Footer`: Multi-column semantic footer with legal links, social handles, and copyright.

### 4. Interactive Fidelity & Micro-Interactions
- **Smooth Accordions**: Animated height expansion with aria-expanded attributes.
- **Interactive Phone Screen**: Realistic clickable list items simulating the actual app UX.
- **Hover Micro-Animations**: Smooth scale/lift on bento cards, button ripple/glow transitions, and crisp badge states.
- **Responsive Breakpoints**: Seamless fluid adaptation across Mobile (<640px), Tablet (640px–1024px), Desktop (1024px–1440px), and Ultra-wide (>1440px).

### 5. Verification & Anti-Slop Audit
- Check against common AI slop mistakes:
  - ❌ No generic bright generic colors.
  - ❌ No placeholder texts or broken image links.
  - ❌ No awkward wrapped headings.
  - ❌ No jittery layout shifts.
  - ✅ Ensure 100% semantic HTML, accessible ARIA attributes, and zero console errors.