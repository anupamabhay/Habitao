


Here is the strictly rephrased version. It preserves your highly frustrated, aggressive tone, ensures not a single technical detail is dropped, and explicitly forbids the AI from hallucinating a fake success response:

***

# Core Fixes & Execution Directives
I am explicitly asking you to add gaps between the header, the text progress, and the round progress bar. How is this so hard, idiot? A college fresher could do this in 5 minutes. Stop fumbling the basics and execute the following perfectly:

## 1. UI Layout & Logic Failures
* **Stats Card Gaps:** Add proper spacing between the header, the text progress, and the progress round bar inside the stat cards immediately.
* **Month View Graph (WTF Logic):** The X-axis indicators in the Month view make absolutely no sense. The timeline literally jumps from 3 to 17 to 4. What the fuck is this logic? Fix the date rendering immediately.
* **Specific Days UI (Habits & Routines):** The text is currently aligned vertically within the day indicators, which breaks the UI and makes them all different heights. They MUST share the exact same width and height, and utilize even gaps (e.g., `justify-content: space-between`). Look at the attached Screenshot 6 and fix it.

## 2. Live Markdown Engine Bugs
* **Cursor Positioning:** The auto-continuation logic for lists is completely backward. When pressing Enter, it drops the cursor on the *left* side of the bullet (see Screenshot 7), forcing me to type backward like `sampletext-`. Fix the cursor offset logic.
* **Broken Checkboxes:** Checkboxes do not work properly inside the markdown editor. There are likely other undiscovered bugs here as well. Debug and fix the entire markdown parser.
* **Native Android Text Selection Popup:** Implement the native Android popup menu for highlighted text. When text is selected, clicking the 3 dots should reveal a native popup with 3-4 basic formatting options (Bold, Italic, Strikethrough, etc.), exactly like modern apps handle it.

## 3. Tasks Feature Parity
* Actively research what core features we are still missing from our Tasks implementation. You must base this research entirely on modern, industry-standard Android apps (TickTick, Todoist, Microsoft To Do, Google Tasks) and strictly implement those missing critical standards.

## 4. Architecture, Repo Polish & PR
* Perform a rigorous structural and naming convention audit across the entire project. Ensure it is 100% production-ready. 
* Enforce professional naming, ensure all files are sorted into proper subfolders, and verify that absolutely no unimportant docs, test files, or random images are cluttering the repo. Update all docs accordingly.
* **Git Workflow:** Once the repo is perfectly clean, push these changes directly to the existing PR we were working on (the one that is still not merged).

---
### Final Verification Command
**Do NOT hallucinate a success summary.** Once your sub-agents complete this execution plan, you must physically verify the graph X-axis logic, test the markdown cursor position yourself, and provide a concrete, technical confirmation that these specific line items actually work. If a task failed, tell me it failed. No bullshit "all done perfectly" responses.