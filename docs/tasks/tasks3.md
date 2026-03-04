## Review & Execution Instructions
Address the Copilot review issues immediately (Reference: `https://github.com/anupamabhay/Habitao/pull/6#issue-4011954191`). Execute the following refinements perfectly across the codebase without dropping any details:

## 1. Global UI (All Pages)
* **Remove Redundant Text:** Strip the text from the `+ new habit`, `+ new task`, and `+ new routine` buttons across all respective screens. Use only the `+` icon, exactly as standard apps do.
* **Page-Specific Stats UI:** The new routine stats screen (located directly on the routines page) is good. You must now implement identical, dedicated stat screens natively on the Habits, Tasks, and Pomodoro pages for their specific metrics. TickTick implements this perfectly—research their UI and strictly copy it.

## 2. Global Stats Page
* **Card Sizing & Alignment:** The "Tasks" and "Daily Goal" cards are completely mismatched in height. Fix their sizing to be identical. Additionally, the text inside them is improperly aligned. Center and fix the alignment.
* **Broken Metrics:** Several stats, particularly the "Habit Consistency" metric in the Insights section, are completely broken or failing to update. Audit every single stat on this page, identify the broken logic, and fix all of them.

## 3. Tasks Page & Architecture
* **Hierarchy Sizing Bug:** The parent tasks currently render visibly smaller than their nested subtasks. Fix this visual hierarchy immediately.
* **Color Bar Distortion:** Because of the aforementioned sizing bug, the priority/color bar on parent tasks is warping/curving more than on the subtasks.
* **TickTick UI Replication:** I want you to thoroughly analyze the attached screenshots (1 and 2) from the TickTick app. They flawlessly handle the UI for tasks, subtasks, descriptions, deadlines, and priorities. You are to copy that exact design. 
* **Rich Descriptions:** Implement support for lists and basic Markdown inside task descriptions, exactly like the reference apps do.
* **Architecture Overhaul:** The tasks feature needs to be significantly better. Synthesize the best concepts, UX, and implementations from TickTick, Todoist, Google Tasks, and Microsoft To Do. Research their architectures and engineer the ultimate, unified Tasks page.
