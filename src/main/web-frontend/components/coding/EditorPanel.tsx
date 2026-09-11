import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import CodeEditor from "./CodeEditor";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type EditorPanelProps = {
  filename: string;
  source: string;
  starterCode: string;
  onSourceChange: (source: string) => void;
};

const fontSizes = [12, 14, 16, 18];

const styles = stylex.create({
  panelHeading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    minHeight: 49,
    padding: "0 24px",
    borderBottom: "1px solid #e8ece5",
    flexShrink: 0
  },
  editorPanel: {
    display: "flex",
    flexDirection: "column",
    minWidth: 0,
    minHeight: 0,
    backgroundColor: "#1d2428",
    color: "#dce3e6"
  },
  editorHeader: {
    borderBottom: "1px solid #344047",
    padding: "0 20px",
    flexWrap: "wrap",
    rowGap: 4
  },
  editorTitle: {
    fontWeight: 500,
    color: "#d0dbd4",
    fontSize: 12,
    display: "flex",
    alignItems: "center",
    gap: 8
  },
  languageDot: {
    width: 7,
    height: 7,
    borderRadius: "50%",
    backgroundColor: "#b79bdd"
  },
  editorTools: {
    display: "flex",
    alignItems: "center",
    gap: 6
  },
  toolButton: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    minHeight: 30,
    minWidth: 30,
    padding: "4px 7px",
    backgroundColor: "transparent",
    border: "1px solid transparent",
    color: {
      default: "#a5b2b9",
      ":hover": "#edf4ef"
    },
    borderRadius: 5,
    cursor: "pointer",
    fontSize: 11,
    outlineOffset: 2,
    userSelect: "none"
  },
  toolActive: {
    backgroundColor: "#34443e",
    color: "#c4dec9",
    borderColor: "#4a6155"
  },
  toolDisabled: {
    opacity: 0.35,
    cursor: "default"
  },
  fontSelect: {
    fontFamily: "inherit",
    fontSize: 11,
    backgroundColor: "#263036",
    color: "#bac6ca",
    border: "1px solid #425159",
    padding: "4px 3px",
    borderRadius: 4,
    cursor: "pointer"
  },
  fileBar: {
    height: 41,
    flexShrink: 0,
    padding: "0 24px",
    display: "flex",
    alignItems: "center",
    gap: 8,
    color: "#91a097",
    fontSize: 11,
    borderBottom: "1px solid #273236"
  },
  fileName: {
    color: "#c5cfc7",
    fontFamily: '"SFMono-Regular", Consolas, monospace'
  },
  editor: {
    flex: 1,
    minHeight: 0,
    minWidth: 0
  },
  editorFooter: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12,
    flexShrink: 0,
    minHeight: 30,
    padding: "0 20px",
    borderTop: "1px solid #344047",
    fontSize: 10,
    color: "#8c9ca2"
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

export default function EditorPanel({filename, source, starterCode, onSourceChange}: EditorPanelProps) {
  const [fontSize, setFontSize] = useState(14);
  const [wordWrap, setWordWrap] = useState(false);
  const [cursor, setCursor] = useState({line: 1, column: 1});

  return (
    <div sx={styles.editorPanel} role="region" aria-labelledby="editor-title">
      <div sx={[styles.panelHeading, styles.editorHeader]}>
        <div role="heading" aria-level={2} id="editor-title" sx={styles.editorTitle}>
          <span sx={styles.languageDot} /> Kotlin Editor
        </div>
        <div sx={styles.editorTools}>
          <Control
            appearance={[styles.toolButton, styles.fontSelect]}
            label={`Editor font size: ${fontSize} pixels. Activate to change size.`}
            title="Cycle editor font size"
            onActivate={() => setFontSize(fontSizes[(fontSizes.indexOf(fontSize) + 1) % fontSizes.length])}
          >
            {fontSize} px
          </Control>
          <Control
            appearance={[styles.toolButton, wordWrap && styles.toolActive]}
            label="Word wrap"
            pressed={wordWrap}
            title="Toggle word wrap"
            onActivate={() => setWordWrap(!wordWrap)}
          >
            <Icon name="wrap" />
          </Control>
          <Control
            appearance={[styles.toolButton, source === starterCode && styles.toolDisabled]}
            label="Reset to starter code"
            title="Reset to starter code (can be undone in the editor)"
            disabled={source === starterCode}
            onActivate={() => onSourceChange(starterCode)}
          >
            <Icon name="reset" />
          </Control>
        </div>
      </div>
      <div sx={styles.fileBar}>
        <Icon name="code" size={14} />
        <span sx={styles.fileName}>{filename}</span>
      </div>
      <div sx={styles.editor}>
        <CodeEditor
          value={source}
          fontSize={fontSize}
          wordWrap={wordWrap}
          onChange={onSourceChange}
          onCursorChange={(line, column) => setCursor({line, column})}
        />
      </div>
      <div sx={styles.editorFooter}>
        <span>Ln {cursor.line}, Col {cursor.column}</span>
        <span>Spaces: 4 <span aria-hidden="true">·</span> UTF-8</span>
      </div>
      <div id="editor-keyboard-help" sx={styles.screenReader}>
        Tab indents code. Press Escape, then Tab to leave the editor. Use your platform’s undo shortcut to undo edits or a reset.
      </div>
    </div>
  );
}
