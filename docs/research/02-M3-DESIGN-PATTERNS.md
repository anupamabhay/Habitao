# Material Design 3 Patterns for Habitao
## Research Date: February 18, 2026

---

## 1. M3 Expressive Overview

Material 3 Expressive (2025-2026) focuses on emotion-driven design:
- Richer color palette with tonal variations
- Shape morphing and corner radius variations
- Size variations to draw attention
- Fluid, natural animations
- Strategic container usage for grouping

### Key Principle for Habitao
Keep it clean and portable. M3 Expressive components are Android-native.
Since we plan iOS support later (Liquid Glass), use M3 patterns that map
well to platform-agnostic concepts:
- Color tinting (works everywhere)
- Elevation/shadow changes (works everywhere)
- Progress indicators (universal)
- Snackbar/toast patterns (universal)
- Avoid Android-only shape morphing that won't translate to iOS

---

## 2. Card Patterns

### M3 Card Variants
- **ElevatedCard**: Default shadow, good for interactive items (USING THIS)
- **FilledCard**: Flat with tonal fill, good for secondary content
- **OutlinedCard**: Border only, good for neutral display

### Card States
- Default: `surfaceContainerLow` background
- Completed: `primaryContainer` with reduced alpha
- Pressed: Slight scale (0.98) with tonal elevation change
- Disabled: Reduced opacity (0.38)

### Card Content Layout (M3 Recommended)
```
+------------------------------------------+
| [Icon/Color]  Title              [Action] |
|               Subtitle                    |
|                                           |
| [Progress Indicator]                      |
| [Supporting Text / Status]                |
+------------------------------------------+
```

---

## 3. Top App Bar Patterns

### LargeTopAppBar (Recommended for Habitao Home)
- Collapsible on scroll
- Large title area for greeting + progress summary
- Scrolls with content for more screen real estate

### Header Content Layout
```
+------------------------------------------+
| Good morning                    [Settings]|
| Wednesday, February 18                    |
|                                           |
| [====----] 3 of 7 done (43%)             |
+------------------------------------------+
```

### Color: Use `surfaceContainer` for app bar background

---

## 4. Progress Indicators

### LinearProgressIndicator (Currently Using)
- Good for measurable habits (3/5 days)
- Use `strokeCap = StrokeCap.Round` for softer look
- Track color: `surfaceVariant`
- Active color: varies by habit type

### CircularProgressIndicator
- Good for daily summary in header (X of Y done)
- Small size (24-32dp) for inline use
- Can combine with text overlay

### Color by Habit Type
- SIMPLE: `primary` (blue/purple)
- MEASURABLE: `secondary` (teal/green)  
- CHECKLIST: `tertiary` (pink/magenta)

---

## 5. Snackbar with Undo (M3 Pattern)

### Standard Undo Snackbar
```kotlin
Snackbar(
    action = {
        TextButton(onClick = onUndo) {
            Text("Undo")
        }
    },
    dismissAction = {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss")
        }
    },
) {
    Text("Habit deleted")
}
```

### Duration
- Use `SnackbarDuration.Short` (4 seconds) for completion undo
- Use `SnackbarDuration.Long` (10 seconds) for delete undo
- Action button text: "Undo" (not "Undo Delete" - keep it short)

---

## 6. Toggle Button Patterns

### FilledIconToggleButton (Currently Using)
- Good for binary state (done/not done)
- checked = completed state
- Use `primary` color for checked, `surfaceVariant` for unchecked

### Size Guidance
- Primary action button: 40dp
- Secondary action button: 34dp (decrement button)
- Minimum touch target: 48dp (ensure padding)

---

## 7. Animation Patterns

### Completion Animation
- Color transition: 300ms tween (currently implemented)
- Scale bounce: 0.9 -> 1.1 -> 1.0 (spring animation, 200ms)
- Checklist expand: `expandVertically` + `shrinkVertically` (currently implemented)

### Card Transitions
- Enter: `fadeIn` + `slideInVertically`
- Exit: `fadeOut` + `slideOutVertically`

### Duration Guidelines (M3)
- Short: 100ms (hover, press feedback)
- Medium: 200-300ms (state changes, toggles)
- Long: 400-500ms (page transitions, expand/collapse)

---

## 8. Color Scheme Usage

### Surface Hierarchy
```
surfaceContainerLowest  -> Background
surfaceContainerLow     -> Card default
surfaceContainer        -> App bar, headers
surfaceContainerHigh    -> Elevated elements
surfaceContainerHighest -> Dialogs, sheets
```

### Habit Type Colors
- SIMPLE: primary / primaryContainer
- MEASURABLE: secondary / secondaryContainer
- CHECKLIST: tertiary / tertiaryContainer

### Completion State
- Active card: `surfaceContainerLow` (default)
- Completed card: `primaryContainer.copy(alpha = 0.4f)` (current)
- Suggestion: Also reduce content alpha to 0.7f for completed text
