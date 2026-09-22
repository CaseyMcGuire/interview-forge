import {useEffect, useImperativeHandle, useRef, type Ref} from "react";
import * as stylex from "@stylexjs/stylex";
import {basicSetup} from "codemirror";
import {Compartment, EditorState} from "@codemirror/state";
import {EditorView, keymap} from "@codemirror/view";
import {indentWithTab, isolateHistory} from "@codemirror/commands";
import {HighlightStyle, indentRange, indentUnit, StreamLanguage, syntaxHighlighting} from "@codemirror/language";
import {kotlin} from "@codemirror/legacy-modes/mode/clike";
import {tags} from "@lezer/highlight";

export type CodeEditorHandle = {
  reformatCode: () => void;
};

type CodeEditorProps = {
  ref?: Ref<CodeEditorHandle>;
  languageKey: string;
  label?: string;
  describedBy?: string;
  value: string;
  fontSize: number;
  wordWrap: boolean;
  onChange: (value: string) => void;
  onCursorChange: (line: number, column: number) => void;
};

// Kotlin Playground's dark surface and Darcula syntax colors, adapted to CodeMirror 6.
// https://play.kotlinlang.org/
const editorTheme = EditorView.theme({
  "&": {height: "100%", backgroundColor: "#1b1b1b", color: "#a9b7c6"},
  "&.cm-focused": {outline: "none"},
  ".cm-scroller": {
    fontFamily: "Menlo, Consolas, monospace",
    lineHeight: "1.4",
    overflow: "auto",
  },
  ".cm-content": {padding: "12px 0", caretColor: "#a9b7c6"},
  ".cm-line": {padding: "0 24px 0 12px"},
  ".cm-gutters": {backgroundColor: "#1b1b1b", color: "#606366", border: "none"},
  ".cm-lineNumbers .cm-gutterElement": {minWidth: "40px", padding: "0 12px"},
  ".cm-activeLine, .cm-activeLineGutter": {backgroundColor: "transparent"},
  ".cm-cursor, .cm-dropCursor": {borderLeftColor: "#a9b7c6"},
  "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection": {
    backgroundColor: "#214283 !important",
  },
  "&.cm-focused .cm-matchingBracket": {backgroundColor: "#3b514d", color: "#ffef28", fontWeight: "bold"},
  "&.cm-focused .cm-nonmatchingBracket": {color: "#bc3f3c"},
  ".cm-selectionMatch": {backgroundColor: "rgba(50, 89, 48, 0.7)", color: "#ffffff"},
  ".cm-searchMatch": {backgroundColor: "rgba(61, 115, 59, 0.7)", color: "#ffffff"},
  ".cm-searchMatch.cm-searchMatch-selected": {backgroundColor: "#3b514d", outline: "1px solid #a9b7c6"},
  ".cm-panels": {backgroundColor: "#313335", color: "#a9b7c6"},
  ".cm-textfield": {backgroundColor: "#1b1b1b", color: "#a9b7c6", borderColor: "#606366"},
  ".cm-button": {backgroundImage: "none", backgroundColor: "#3b3e3f", color: "#a9b7c6"},
  ".cm-tooltip": {backgroundColor: "#3b3e3f", color: "#9c9e9e", border: "1px solid #606366"},
  ".cm-tooltip-autocomplete > ul > li[aria-selected]": {backgroundColor: "#494d4e", color: "#9c9e9e"},
}, {dark: true});

const editorHighlighting = HighlightStyle.define([
  {tag: tags.keyword, color: "#cc7832", fontWeight: "700"},
  {tag: [tags.bool, tags.null, tags.atom], color: "#cc7832"},
  {tag: [tags.typeName, tags.className], color: "#aabbcc", fontWeight: "bold"},
  {tag: [tags.variableName, tags.operator, tags.bracket], color: "#a9b7c6"},
  {tag: tags.definition(tags.variableName), color: "#a9b7c6", fontStyle: "italic"},
  {tag: tags.standard(tags.variableName), color: "#ff9e59"},
  {tag: tags.propertyName, color: "#ffc66d"},
  {tag: tags.number, color: "#6897bb"},
  {tag: tags.string, color: "#6a8759"},
  {tag: tags.comment, color: "#61a151", fontStyle: "italic"},
  {tag: tags.meta, color: "#bbb529"},
  {tag: tags.invalid, color: "#bc3f3c"},
]);

const styles = stylex.create({
  container: {
    height: "100%",
    minHeight: 0,
    minWidth: 0
  },
});

export default function CodeEditor(props: CodeEditorProps) {
  const {
    ref,
    languageKey,
    label = "Code editor",
    describedBy = "editor-keyboard-help",
    value,
    fontSize,
    wordWrap,
    onChange,
    onCursorChange,
  } = props;
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const initialValue = useRef(value);
  const changeCallback = useRef(onChange);
  const cursorCallback = useRef(onCursorChange);
  const fontCompartment = useRef(new Compartment());
  const wrapCompartment = useRef(new Compartment());
  const languageCompartment = useRef(new Compartment());
  const attributesCompartment = useRef(new Compartment());

  useImperativeHandle(ref, () => ({
    reformatCode() {
      const view = viewRef.current;
      if (!view) {
        return;
      }

      view.dispatch({
        changes: indentRange(view.state, 0, view.state.doc.length),
        annotations: isolateHistory.of("full"),
      });
      view.focus();
    },
  }), []);

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
          languageCompartment.current.of([]),
          syntaxHighlighting(editorHighlighting),
          editorTheme,
          indentUnit.of("  "),
          EditorState.tabSize.of(2),
          keymap.of([indentWithTab]),
          attributesCompartment.current.of([]),
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

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: [
        // Languages without an installed parser still have a usable plain-text editor.
        languageCompartment.current.reconfigure(languageKey === "kotlin" ? StreamLanguage.define(kotlin) : []),
        attributesCompartment.current.reconfigure(EditorView.contentAttributes.of({
          "aria-label": label,
          "aria-describedby": describedBy,
          spellcheck: "false",
        })),
      ],
    });
  }, [languageKey, label, describedBy]);

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
