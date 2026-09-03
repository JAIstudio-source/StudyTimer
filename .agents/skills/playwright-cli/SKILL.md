---
name: playwright-cli
description: >-
  Use Microsoft's Playwright CLI (@playwright/cli) for token-efficient browser automation,
  inspection, testing, clicking, typing, taking screenshots, and recording user sessions directly from the terminal.
---

# Playwright CLI Skill (@playwright/cli)

Playwright CLI provides token-efficient browser automation designed specifically for AI agents.

## Common Workflows

### 1. Open and Navigate
```bash
playwright-cli open https://localhost:3000
playwright-cli goto https://localhost:3000/settings
```

### 2. Inspect and Snapshot
```bash
# Capture page snapshot to get element references
playwright-cli snapshot

# Search page for specific elements or text
playwright-cli find "Submit"
```

### 3. User Interactions
```bash
playwright-cli click <target>
playwright-cli fill <target> "text"
playwright-cli select <target> "option"
playwright-cli check <target>
```

### 4. Visual Verification & Screenshots
```bash
playwright-cli screenshot
playwright-cli screenshot <target>
```

### 5. Network and DevTools
```bash
playwright-cli console
playwright-cli requests
playwright-cli request <index>
```

### 6. Cleanup
```bash
playwright-cli close
playwright-cli close-all
```
