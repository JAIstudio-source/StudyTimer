# StudyTimer Requirements & Test Specifications

## Functional Testing Requirements (from testing.md)

### Module A: Timer Modes & Time Calculations
- [ ] Stopwatch Mode: Start, pause, resume, hold-to-finish. Verify elapsed time format (`00:00:00`).
- [ ] Target Countdown Mode: Selection, countdown precision, alert handling.
- [ ] Pomodoro Mode: Work/break cycle switching, auto-break triggers, long break intervals.
- [ ] Lecture Mode: Fixed class schedule duration locking, active lecture binding.
- [ ] Midnight Rollover & Boundary Verification: Session constraints, gap capping, zero-jump timer behavior.

### Module B: Subject & Tag Management
- [ ] Subject Selection: Switching active subject tag on Focus panel.
- [ ] Custom Subject Creation: Emoji selector, color picker, custom name validation.
- [ ] Subject Deletion & Re-assignment: Delete custom subject with historical focus time, reassign to General.
- [ ] Subject Re-addition & Hidden Subject Resolution: Clean unhiding without duplicate key conflicts.

### Module C: Insights, Stats Engine & Data Integrity
- [ ] Self-Healing Sanitization: Verify `sanitizeAndHealHistoricalTotals()` auto-corrects corrupted data.
- [ ] Timeline Logging: Creation, editing, deduction, and JSON serialization.
- [ ] Pie Chart & Segment Ring: Distribution percentage verification matching raw duration totals.
- [ ] Streak & Active Days Calculation: Streak continuation, grace period rules.

### Module D: Leaderboard, Supabase & Cloud Sync
- [ ] Leaderboard Rank Calculation: Focus score, user display name formatting.
- [ ] Profile Sync: Syncing local metrics to cloud database cleanly.

### Module E: Planner, Goals & Exam Countdowns
- [ ] Goal Scheduler: Creation, progress tracking against active timers.
- [ ] Exam Countdown Manager: Date calculation accuracy, days remaining formatting.

## UI/UX & Quality Requirements
- [ ] Visual Spacing & Grid Alignment (48dp min touch targets).
- [ ] Contrast & Dark/AMOLED Mode (4.5:1 min contrast ratio).
- [ ] Typography, Copywriting & Text Scaling.
- [ ] Animation, Fluidity & Micro-Interactions (`TimerRingView`, `HoldRingButton`).
