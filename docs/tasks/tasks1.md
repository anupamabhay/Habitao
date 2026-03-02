# Execution Strategy & Instructions
Stop guessing and start researching. For every feature, fix, and improvement listed below, you must actively research how top-tier apps handle these exact mechanics, read the latest docs/guides, and implement the optimal approach. 

Strictly delegate these tasks, aggressively monitor your sub-agents (retry them immediately if they fail, get stuck, or stop responding), and rigorously review their code before finalizing.

## 1. Routines (Logic & UI Failures)
* **Broken Refresh Logic:** Routines completely fail to repeat or refresh. Marking a daily routine as complete on Monday permanently marks it done for every other day. Fix this core logic immediately.
* **Missing History/Future Context:** There is no way to check past or future routines. Add a stats icon to the top right of the routines page that opens a dedicated page displaying all routine stats and timeline context.
* **Inconsistent UI:** Text sizing is completely non-uniform across the page. Standardize text sizes globally.
* **Cluttered Design:** The UI is still way too busy. Implement proper padding, margins, alignment, and gaps throughout the entire app to make it breathe.

## 2. Pomodoro (UX Overhaul)
* **Missing Session Indicators:** There is zero textual indication whether the timer is a Work Session, Short Break, or Long Break. Relying solely on the dynamic theme circle color is bad UX. Research and implement a clean UI indicator (like a small text label).
* **Immersive View is Wrong:** The immersive view must utilize the Always On Display (AOD) exactly like TickTick, rather than keeping the entire actual display on. Research TickTick's AOD implementation and strictly copy it.
* **Horrid Switch UI:** The switch style for the immersive view is terrible. Replace it with a swipeable interface. Users must be able to swipe left/right to cycle through the available AOD-based view styles (again, exactly like TickTick).

## 3. Tasks (Priority & Editing Mechanics)
* **Hidden Priority Indicators:** Priority indicators disappear on tasks with long titles. Fix this. Stop using a detached dot. Move the indicator to the left and make it blend seamlessly with the UI—color-code the checkbox or add a thin vertical color bar (`|`) next to the task, identical to Todoist or TickTick.
* **Interactive Editing & Subtasks:** Task editing must be deeply interactive. Allow users to add checkboxes directly inside descriptions. Implement a true subtask hierarchy (child tasks that can independently have their own descriptions and nested subtasks). Google Tasks and TickTick handle this flawlessly. Research their structure and implement it efficiently.

## 4. Habits (Calendar Glitch)
* **Broken Scroll Mechanics:** Calendar scrolling in Habits is entirely broken. Scrolling left or right quickly causes the dates to fail to catch up, fail to roll back to the current week, or severely glitch the UI. Make this logic absolutely robust.

## 5. Stats (Performance & Graph Logic)
* **Terrible Performance:** The stats page lags horribly. Execute heavy optimizations. Ensure the entire app supports and utilizes the highest available device refresh rates to eliminate lag. Research modern app performance standards and apply them.
* **Broken Graph Logic:** The horizontal (X) and vertical (Y) axes must be sticky/always visible. The user should not have to scroll to find the units.
* **Scrollable Day View:** Make the Day view horizontally scrollable to properly fit all 24-hour timestamps, similar to how the Month view behaves.
* **Overlapping Bars:** The bar graph logic is flawed. Bars actively overlap when Habits, Routines, and Tasks are completed simultaneously. Debug and fix this completely.
* **Rich Analytics:** Add significantly better, richer stats for Pomodoro, Tasks, and Habits. Look at TickTick’s stats page, research their metrics, and replicate that level of detail.

## 6. Navigation (Alignment & Icons)
* **Duplicate Icons:** Habits and Tasks are currently using basically the identical icons. Change them to distinct, appropriate icons.
* **Crooked UI:** The navigation icons and text are visibly misaligned. Fix the centering and alignment perfectly.
* **Tab Labels:** Add a functional toggle inside the Settings page (under "Tab Settings") to enable/disable navigation tab names.

## 7. Settings & Global App Polish
* **Settings Architecture:** The "About Habitao" and "Authors" options must open a dedicated new page displaying those details, rather than awkwardly showing them as descriptions under the setting option.
* **Permissions:** Ensure the app actually requests *all* required system permissions from the user.
* **Functional Toggles:** Make the "Notifications" and "Focus & Pomodoro" settings actually work. (Link the global Pomodoro settings directly to the Pomodoro page's local settings).
* **Status Bar Bug:** The global status bar coloring (dark/light mode) randomly fails to update. Fix this inconsistency.
* **Future Planning:** Draft a concrete technical plan for a robust backup, restore, and data sync feature for future implementation.