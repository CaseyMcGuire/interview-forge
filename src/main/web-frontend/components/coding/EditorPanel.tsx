import * as stylex from "@stylexjs/stylex";
import CodeEditor from "./CodeEditor";

type EditorPanelProps = {
  languageKey: string;
  languageName: string;
  source: string;
  fontSize: number;
  wordWrap: boolean;
  onSourceChange: (source: string) => void;
  onCursorChange: (line: number, column: number) => void;
};

const styles = stylex.create({
  editorPanel: {
    flexGrow: 1,
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#2b2b2b",
    color: "#a9b7c6"
  },
  editor: {
    flexGrow: 1,
    minHeight: 0,
    minWidth: 0
  },
  screenReader: {
    position: "absolute",
    width: 1,
    height: 1,
    padding: 0,
    margin: -1,
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
    borderWidth: 0
  }
});

/** The editor fills its column edge to edge; its tools live in the toolbar and its facts in the status bar. */
export default function EditorPanel({
  languageKey,
  languageName,
  source,
  fontSize,
  wordWrap,
  onSourceChange,
  onCursorChange,
}: EditorPanelProps) {
  return (
    <div sx={styles.editorPanel} role="region" aria-label={`${languageName} editor`}>
      <div sx={styles.editor}>
        <CodeEditor
          languageKey={languageKey}
          label={`${languageName} code editor`}
          value={source}
          fontSize={fontSize}
          wordWrap={wordWrap}
          onChange={onSourceChange}
          onCursorChange={onCursorChange}
        />
      </div>
      <div id="editor-keyboard-help" sx={styles.screenReader}>
        Tab indents code. Press Escape, then Tab to leave the editor. Use your platform’s undo shortcut to undo edits or a reset.
      </div>
    </div>
  );
}
