all screenshots are in @docs\output 

# UI/UX Overhaul & Execution Directives
Get your shit together, because the design implementation on these latest updates is still terrible. Stop making up bad layouts and perfectly execute the following fixes. Look at the provided screenshots, use common sense, and research how modern Android apps actually handle these elements:

## 1. Routines (Specific Days Logic)
* The "specific days" implementation you just built for the Routines page is garbage. Stop reinventing the wheel. Just copy the exact implementation from the Habits page—it already uses a proper 2-row by 2-column layout. Just copy it, bruh.

## 2. Stats Page Layout
* The stats cards aren't cramped anymore, but the internal layout of the content still looks bad. Fix the visual hierarchy within each card: 
  * Keep the header exactly where it is at the top.
  * Move the stat text directly below the header.
  * Push the progress icons to the very bottom of the card.
  * use proper margin, padding, alignment.

## 3. Global UI Consistency & Navigation Bar
* The "Create [X]" buttons are wildly inconsistent across different pages and completely fail to blend with the navigation gesture bar. Research modern Android UI standards immediately. 
* The button area does *not* need a separate, clashing background. The app's main background should flow seamlessly behind the button and into the bottom navigation/gesture bar area, exactly how standard modern apps do it. 
* These buttons MUST be 100% identical and consistent (shape, size, padding, alignment) across *every single page*. 
* **Global Audit:** I've noticed several UI inconsistencies for similar components across different pages. Fix all of them. Consistent design style is incredibly important.

## 4. Chart Graphs & Misalignment
* The chart bars finally have gaps, but they are still way too thick. 
* The main issue: the bars are now overflowing and completely misaligned with the day units on the bottom X-axis (check the screenshot). 
* **Fix:** Either make the graph horizontally scrollable in the Day view (just like the other views), or calculate the sizing properly so everything fits and aligns perfectly on one screen. Ensure the rendering remains highly optimized and fast.

## 5. Markdown Implementation
* Your markdown implementation is completely wrong and not what I asked for. TickTick does not use a clunky, separate preview mode. It uses a live preview that renders *directly inside the editable component* as you type. Look at the screenshot, research how to build a live WYSIWYG markdown editor in Android, and do it exactly like that.

---
**FINAL REQUIREMENT:** When you are done, do not give me a hallucinated summary saying everything is perfect. Verify the UI alignments yourself, test the live markdown, check the nav bar blending, and give me a concrete, technical confirmation of what was actually fixed.

@docs\output 