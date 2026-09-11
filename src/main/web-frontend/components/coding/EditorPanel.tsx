import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import CodeEditor from "./CodeEditor";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type EditorPanelProps = {
  languageName: string;
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
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40",
    flexShrink: 0
  },
  editorPanel: {
    display: "flex",
    flexDirection: "column",
    minWidth: 0,
    minHeight: 0,
    backgroundColor: "#1e1f22",
    color: "#bcbec4"
  },
  editorHeader: {
    backgroundColor: "#2b2d30",
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40",
    padding: "0 20px",
    flexWrap: "wrap",
    rowGap: 4
  },
  editorTitle: {
    fontWeight: 500,
    color: "#dfe1e5",
    fontSize: 12,
    display: "flex",
    alignItems: "center",
    gap: 8
  },
  languageDot: {
    width: 7,
    height: 7,
    borderRadius: "50%",
    backgroundColor: "#b589ec"
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
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "transparent",
    color: {
      default: "#9da0a8",
      ":hover": "#dfe1e5"
    },
    borderRadius: 5,
    cursor: "pointer",
    fontSize: 11,
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2,
    userSelect: "none"
  },
  toolActive: {
    backgroundColor: "#2e436e",
    color: "#dfe1e5",
    borderColor: "#375fad"
  },
  toolDisabled: {
    opacity: 0.35,
    cursor: "default"
  },
  fontSelect: {
    fontFamily: "inherit",
    fontSize: 11,
    backgroundColor: "#393b40",
    color: "#b4b8bf",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
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
    color: "#b589ec",
    fontSize: 11,
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40"
  },
  fileName: {
    color: "#dfe1e5",
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
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#393b40",
    backgroundColor: "#2b2d30",
    fontSize: 10,
    color: "#9da0a8"
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

export default function EditorPanel({languageName, filename, source, starterCode, onSourceChange}: EditorPanelProps) {
  const [fontSize, setFontSize] = useState(14);
  const [wordWrap, setWordWrap] = useState(false);
  const [cursor, setCursor] = useState({line: 1, column: 1});

  return (
    <div sx={styles.editorPanel} role="region" aria-labelledby="editor-title">
      <div sx={[styles.panelHeading, styles.editorHeader]}>
        <div role="heading" aria-level={2} id="editor-title" sx={styles.editorTitle}>
          <span sx={styles.languageDot} /> {languageName} Editor
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
