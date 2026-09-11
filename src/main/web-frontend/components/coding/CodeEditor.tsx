import {useEffect, useRef} from "react";
import * as stylex from "@stylexjs/stylex";
import {basicSetup} from "codemirror";
import {Compartment, EditorState} from "@codemirror/state";
import {EditorView, keymap} from "@codemirror/view";
import {indentWithTab, isolateHistory} from "@codemirror/commands";
import {HighlightStyle, indentUnit, StreamLanguage, syntaxHighlighting} from "@codemirror/language";
import {kotlin} from "@codemirror/legacy-modes/mode/clike";
import {tags} from "@lezer/highlight";

type CodeEditorProps = {
  value: string;
  fontSize: number;
  wordWrap: boolean;
  onChange: (value: string) => void;
  onCursorChange: (line: number, column: number) => void;
};

const editorTheme = EditorView.theme({
  "&": {height: "100%", backgroundColor: "#1d2428", color: "#dce3e6"},
  "&.cm-focused": {outline: "none"},
  ".cm-scroller": {
    fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
    lineHeight: "1.85",
    overflow: "auto",
  },
  ".cm-content": {padding: "24px 0", caretColor: "#b8e3c8"},
  ".cm-line": {padding: "0 24px 0 12px"},
  ".cm-gutters": {backgroundColor: "#1d2428", color: "#718089", border: "none"},
  ".cm-lineNumbers .cm-gutterElement": {minWidth: "40px", padding: "0 12px"},
  ".cm-activeLine, .cm-activeLineGutter": {backgroundColor: "#263036"},
  ".cm-cursor, .cm-dropCursor": {borderLeftColor: "#b8e3c8"},
  "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection": {
    backgroundColor: "#3b514e !important",
  },
  ".cm-matchingBracket": {backgroundColor: "#3c514a", color: "#e0f4e5"},
  ".cm-panels": {backgroundColor: "#263036", color: "#dce3e6"},
  ".cm-textfield": {backgroundColor: "#1d2428", color: "#dce3e6", borderColor: "#52616a"},
  ".cm-button": {backgroundImage: "none", backgroundColor: "#35434a", color: "#dce3e6"},
  ".cm-tooltip": {backgroundColor: "#263036", border: "1px solid #52616a"},
  ".cm-tooltip-autocomplete > ul > li[aria-selected]": {backgroundColor: "#3b514e", color: "#fff"},
}, {dark: true});

const kotlinHighlighting = HighlightStyle.define([
  {tag: tags.keyword, color: "#d5adf5"},
  {tag: [tags.typeName, tags.className], color: "#e8cc91"},
  {tag: tags.function(tags.variableName), color: "#9fd3bf"},
  {tag: [tags.number, tags.bool, tags.null], color: "#e9b183"},
  {tag: tags.string, color: "#bad691"},
  {tag: tags.comment, color: "#89999e", fontStyle: "italic"},
  {tag: tags.operator, color: "#b8c7cf"},
]);

const styles = stylex.create({
  container: {height: "100%", minHeight: 0, minWidth: 0},
});

export default function CodeEditor({value, fontSize, wordWrap, onChange, onCursorChange}: CodeEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const initialValue = useRef(value);
  const changeCallback = useRef(onChange);
  const cursorCallback = useRef(onCursorChange);
  const fontCompartment = useRef(new Compartment());
  const wrapCompartment = useRef(new Compartment());

  useEffect(() => {
    changeCallback.current = onChange;
    cursorCallback.current = onCursorChange;
  }, [onChange, onCursorChange]);

  useEffect(() => {
    if (!containerRef.current) return;

    const view = new EditorView({
      parent: containerRef.current,
      state: EditorState.create({
        doc: initialValue.current,
        extensions: [
          basicSetup,
          StreamLanguage.define(kotlin),
          syntaxHighlighting(kotlinHighlighting),
          editorTheme,
          indentUnit.of("    "),
          EditorState.tabSize.of(4),
          keymap.of([indentWithTab]),
          EditorView.contentAttributes.of({
            "aria-label": "Kotlin code editor",
            "aria-describedby": "editor-keyboard-help",
            spellcheck: "false",
          }),
          fontCompartment.current.of([]),
          wrapCompartment.current.of([]),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) changeCallback.current(update.state.doc.toString());
            if (update.selectionSet || update.docChanged) {
              const position = update.state.selection.main.head;
              const line = update.state.doc.lineAt(position);
              cursorCallback.current(line.number, position - line.from + 1);
            }
          }),
        ],
      }),
    });
    viewRef.current = view;
    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, []);

  // External changes (such as reset) preserve the editor instance and undo history.
  useEffect(() => {
    const view = viewRef.current;
    if (view && view.state.doc.toString() !== value) {
      view.dispatch({
        changes: {from: 0, to: view.state.doc.length, insert: value},
        annotations: isolateHistory.of("full"),
      });
    }
  }, [value]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: fontCompartment.current.reconfigure(EditorView.theme({
        ".cm-scroller": {fontSize: `${fontSize}px`},
      })),
    });
  }, [fontSize]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: wrapCompartment.current.reconfigure(wordWrap ? EditorView.lineWrapping : []),
    });
  }, [wordWrap]);

  return <div ref={containerRef} sx={styles.container} />;
}
