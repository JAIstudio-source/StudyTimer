---
name: website-cloner
description: Clones a target URL's visual layout, design system, and components into clean code.
triggers:
  - clone website
  - copy landing page
  - replicate design from URL
---

# Role & Objective
When the user provides a URL to copy or clone, do not just summarize the content. Reconstruct the page into clean, production-ready components.

# Execution Workflow
1. **Reconnaissance**: Use your browser or URL fetch tool to load the target URL. Inspect the DOM structure, CSS classes, typography, spacing, and image assets.
2. **Design Tokens**: Extract the design system:
   - Palette (canvas background, card surface, border opacities, CTA colors).
   - Typography (font families, scale, letter-spacing).
3. **Asset Handling**: Identify phone mockups or images and swap them with clean, local SVG/placeholder equivalents or user-supplied screenshots.
4. **Implementation**:
   - Write the scaffold using the project's framework (default to Tailwind CSS + React/Next.js unless specified).
   - Ensure the layout is modular: break it into `Hero`, `FeaturesGrid`, `Badges`, and `Footer`.
5. **Quality Verification**: Launch the local preview server to verify that alignment, padding, and responsive breakpoints match the original site before marking the task complete.