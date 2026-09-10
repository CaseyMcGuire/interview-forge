# 0013: CodeMirror 6 for the coding workspace

The coding workspace uses CodeMirror 6, as requested for the interview practice editor.
A small React adapter owns and destroys the EditorView, synchronizes external document
changes, and uses compartments to change font size and line wrapping without losing
selection or undo history. Kotlin highlighting uses CodeMirror's Kotlin stream parser
from `@codemirror/legacy-modes`; the editor itself is CodeMirror 6.

Workspace layout uses divs and spans, with ARIA roles and keyboard activation for controls,
and the existing StyleX pipeline. CodeMirror's extension-based theme
styles its own managed DOM. The editor includes basic editing, search, history, bracket
matching, and indentation. This does not provide Kotlin compiler diagnostics or a language server.

The initial page uses a sample problem and local browser drafts. Run Tests and Submit
remain disabled until a real execution service is connected; expected outputs are
clearly separated from actual results. No database migration or execution API is added.
The preview storage key is scoped to the sample problem and language. When authenticated
problem data is connected, draft storage must additionally be scoped by viewer identity.

**Trade-offs:** The stream parser is sufficient for syntax highlighting, but semantic
completion and diagnostics will need additional language tooling. Browser drafts survive
reloads on the same device but do not sync between accounts or devices.
