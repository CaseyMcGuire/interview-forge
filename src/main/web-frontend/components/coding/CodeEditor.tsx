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
  "&": {height: "100%", backgroundColor: "#1e1f22", color: "#bcbec4"},
  "&.cm-focused": {outline: "none"},
  ".cm-scroller": {
    fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
    lineHeight: "1.85",
    overflow: "auto",
  },
  ".cm-content": {padding: "24px 0", caretColor: "#ced0d6"},
  ".cm-line": {padding: "0 24px 0 12px"},
  ".cm-gutters": {backgroundColor: "#1e1f22", color: "#6f737a", border: "none"},
  ".cm-lineNumbers .cm-gutterElement": {minWidth: "40px", padding: "0 12px"},
  ".cm-activeLine, .cm-activeLineGutter": {backgroundColor: "#26282e"},
  ".cm-activeLineGutter": {color: "#a1a3ab"},
  ".cm-cursor, .cm-dropCursor": {borderLeftColor: "#ced0d6"},
  "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection": {
    backgroundColor: "#214283 !important",
  },
  ".cm-matchingBracket": {backgroundColor: "#3b514d", color: "#dfe1e5"},
  ".cm-nonmatchingBracket": {backgroundColor: "#5e3838", color: "#eb938d"},
  ".cm-selectionMatch": {backgroundColor: "#373b39"},
  ".cm-searchMatch": {backgroundColor: "#5e4d33", outline: "1px solid #826a41"},
  ".cm-searchMatch.cm-searchMatch-selected": {backgroundColor: "#825845"},
  ".cm-panels": {backgroundColor: "#2b2d30", color: "#bcbec4"},
  ".cm-textfield": {backgroundColor: "#1e1f22", color: "#bcbec4", borderColor: "#4e5157"},
  ".cm-button": {backgroundImage: "none", backgroundColor: "#393b40", color: "#bcbec4"},
  ".cm-tooltip": {backgroundColor: "#2b2d30", border: "1px solid #4e5157"},
  ".cm-tooltip-autocomplete > ul > li[aria-selected]": {backgroundColor: "#2e436e", color: "#dfe1e5"},
}, {dark: true});

const kotlinHighlighting = HighlightStyle.define([
  {tag: [tags.keyword, tags.bool, tags.null, tags.atom], color: "#cf8e6d"},
  {tag: [tags.typeName, tags.className], color: "#bcbec4"},
  {tag: [tags.function(tags.variableName), tags.definition(tags.variableName)], color: "#56a8f5"},
  {tag: tags.number, color: "#2aacb8"},
  {tag: tags.string, color: "#6aab73"},
  {tag: tags.comment, color: "#7a7e85"},
  {tag: tags.meta, color: "#b3ae60"},
  {tag: tags.operator, color: "#bcbec4"},
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
