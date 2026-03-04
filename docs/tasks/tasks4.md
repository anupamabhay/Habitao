
# Core Directives & Execution Standards
Stop assuming these tasks are easy and dropping the ball. For every single feature, bug fix, and optimization detailed below, you must actively verify how modern standard apps (like TickTick, Todoist, etc.) handle them, consult current online documentation, and rigidly implement the absolute best execution. Ensure you actively delegate, strictly monitor your agents, aggressively retry them when they fail or stall, and rigorously review all logic before signing off on it.

## 1. Routines Logic & UI
* **Color Status Mismatch:** The priority color drops to a mismatch state—setting low priority during creation shows green, but once created it switches to blue. Standardize this color mapping.
* **Repeat Logic Check:** Routines need an option allowing users to choose specific repeating days, precisely like Habits.
* **Stats Page Layout Fail:** By shrinking the rectangular cards, you clustered the text, progress components, and headers together into a messy clump without utilizing proper padding or margins. Look at the screenshots, apply standard design common sense, and fix the internal padding and layout alignment.

## 2. Tasks Layout & Formatting
* **Spacing & Alignment:** While the Task page is much improved, it still feels congested. Adjust padding, alignment, and positioning so components have appropriate (but not excessive) distance from each other.
* **Markdown Implementation:** Markdown inside task descriptions is broken. It must render correctly during *both* editing and viewing (just like TickTick). Research markdown parser implementations and fix it.

---

### Final Verification Command
**Do NOT hallucinate a success summary.** Once your sub-agents complete this execution plan, you must physically verify the logic, run the environment tests, and provide a concrete, technical confirmation that these specific line items work. If a task failed, you tell me it failed. No bullshit "all done perfectly" responses.