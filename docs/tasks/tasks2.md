


Here is the strictly rephrased version. It maintains your highly demanding, authoritative tone, preserves every single technical detail, and sets rigid expectations for the AI agent’s execution:

***

# Execution Strategy & Instructions
Stop guessing, do proper research on how top-tier apps handle these exact mechanics, and copy their implementations flawlessly. After implementing, heavily optimize the codebase. Delegate properly, monitor execution, and fix the following immediately:

## 1. Pomodoro (Clutter, AOD & UX Failures)
* **Cluttered Top Text:** The top UI is a mess. Having two "Focus >" texts (one for the task selector, one for the session title) is terrible UX. Change the task selector to say "Task >" (or similar if no task is selected) and move it below the timer and above the button, OR move the session count there instead. Fix the visual hierarchy.
* **Fake AOD Implementation:** The immersive window is just pitch black right now. I want an *actual* Always On Display (AOD) implementation like TickTick. The screen brightness must actively decrease, and you must add their tomato timer animation as the 4th style option (refer to screenshot 1).
* **Missing Time Adjustments:** In TickTick, you can click the active timer to instantly add or reduce time (refer to screenshot 2). Copy-paste this exact feature.
* **Broken Close Button:** The "X" close button on the immersive window is completely dead; it fails to close the view. Fix it immediately.

## 2. Stats Screen (Logic Bugs & MD3 Upgrades)
* **Broken Focus Timings Logic:** The stats screen fails to update focus timings properly. If I skip a timer after some time has passed, that accumulated time MUST still be counted. Fix this logic flaw.
* **Inconsistent UI Components:** The rectangular cards are improperly sized (all smaller cards must be identical in size). Upgrade these components to modern Material Design 3 (MD3) standards using properly curved rectangles.
* **Dead Stats:** Some stats still completely fail to work. I expect *every single stat* on this page to function correctly, display accurate data, and update in real-time.

## 3. Settings (Duplicate Code & Broken Toggles)
* **Duplicate Settings Code:** The Focus/Pomodoro settings inside the main Settings page use completely different code than the settings on the actual Pomodoro page. Stop duplicating code. Link the Pomodoro page's settings directly to the main Settings page so the exact same component/logic is reused.
* **Dead Toggles:** The notification toggles in the settings do absolutely nothing. Wire them up to work.
* **Hardcoded App Version:** The app version currently says a hardcoded "v1.0", while the actual GitHub releases are at v0.1.4 (and this will be 0.1.5). Make this version number dynamically pull from the actual release/build version.
* **Cramped Popup UI:** The settings popup that slides up from the bottom (when clicking the 3-dots navigation icon) has cards that are way too small. Increase the size and add proper padding (refer to screenshot 5).

## 4. Navigation (Alignment & SafeArea Overlaps)
* **Crooked Icons:** Verify that *all* icons and labels in the navigation bar are the exact same size and perfectly aligned. Currently, they become crookedly aligned on smaller screens when 5 icons are present (refer to screenshot 3). It works fine when labels are disabled (refer to screenshot 4), but the with-labels view must be fixed.
* **System Nav Bar Overlap:** The "Create Habit/Task" bottom buttons physically overlap with the Android system navigation bar (the swipe gesture area) at times. Research how modern apps handle safe-area insets/padding. Add proper margin/padding, or keep the color blended while safely pushing the app's interactive bottom area *above* the Android navigation pill.