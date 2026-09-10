import {useEffect, useState, type ReactNode} from "react";
import type {StyleXStyles} from "@stylexjs/stylex";
import CodeEditor from "./CodeEditor";
import {sampleProblem as problem} from "./sampleProblem";
import {styles} from "./workspaceStyles";

// This is a preview draft. Authenticated workspaces must also scope this key by viewer ID.
const draftKey = `interview-forge:preview-draft:${problem.slug}:kotlin:v1`;

function readDraft() {
  try {
    return {source: localStorage.getItem(draftKey) ?? problem.starterCode, available: true};
  } catch {
    return {source: problem.starterCode, available: false};
  }
}

const iconGlyphs = {
  document: "▤", code: "‹›", reset: "↶", wrap: "↵",
  play: "▷", submit: "↑", check: "✓", terminal: "›_", clock: "◷",
};

function Icon({name, size = 16}: {name: keyof typeof iconGlyphs; size?: number}) {
  return <span sx={styles.icon(size)} aria-hidden="true">{iconGlyphs[name]}</span>;
}

function Control({children, onActivate, disabled, label, description, pressed, controls, appearance, title}: {
  children: ReactNode;
  onActivate?: () => void;
  disabled?: boolean;
  label?: string;
  description?: string;
  pressed?: boolean;
  controls?: string;
  appearance: StyleXStyles;
  title?: string;
}) {
  return (
    <div role="button" tabIndex={disabled ? -1 : 0} sx={appearance}
      aria-label={label} aria-disabled={disabled} aria-describedby={description}
      aria-pressed={pressed} aria-controls={controls} title={title}
      onClick={disabled ? undefined : onActivate}
      onKeyDown={(event) => {
        if (!disabled && (event.key === "Enter" || event.key === " ")) {
          event.preventDefault();
          onActivate?.();
        }
      }}>
      {children}
    </div>
  );
}

const fontSizes = [12, 14, 16, 18];

export default function CodingWorkspace() {
  const [draft, setDraft] = useState(readDraft);
  const [fontSize, setFontSize] = useState(14);
  const [wordWrap, setWordWrap] = useState(false);
  const [cursor, setCursor] = useState({line: 1, column: 1});
  const [selectedCase, setSelectedCase] = useState(0);
  const example = problem.examples[selectedCase];

  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${problem.title} · Interview Forge`;
    return () => {document.title = previousTitle;};
  }, []);

  function updateSource(source: string) {
    let available = true;
    try {
      localStorage.setItem(draftKey, source);
    } catch {
      available = false;
    }
    setDraft({source, available});
  }

  return (
    <div sx={styles.page}>
      <div sx={styles.header}>
        <div sx={styles.brand}>
          <span sx={styles.brandMark} aria-hidden="true"><Icon name="code" size={21} /></span>
          Interview Forge
        </div>
        <div sx={styles.headerContext}>
          <span sx={styles.contextLabel}>Coding practice</span>
          <span sx={styles.previewBadge}>Editor preview</span>
        </div>
      </div>

      <div sx={styles.workspace} role="main">
        <div sx={styles.problem} role="region" aria-labelledby="problem-title">
          <div sx={styles.panelHeading}>
            <span sx={styles.panelLabel}><Icon name="document" /> Problem</span>
            <span sx={styles.muted}>Sample problem</span>
          </div>
          <div sx={styles.problemBody} tabIndex={0} aria-label="Problem description">
            <div sx={styles.eyebrow}>Arrays &amp; hashing</div>
            <div role="heading" aria-level={1} id="problem-title" sx={styles.title}>{problem.title}</div>
            <div sx={styles.badges}>
              <span sx={[styles.badge, styles.easyBadge]}>Easy</span>
              <span sx={styles.badge}>Array</span>
              <span sx={styles.badge}>Hash map</span>
            </div>
            <div sx={styles.paragraph}>
              Find two distinct positions in <span sx={styles.inlineCode}>nums</span> whose values
              sum to <span sx={styles.inlineCode}>target</span>.
            </div>
            <div sx={styles.paragraph}>
              Return those positions as a two-element array. The order of the positions doesn’t matter.
              Each input has exactly one matching pair.
            </div>

            <div role="heading" aria-level={2} sx={styles.sectionHeading}>Requirements</div>
            <div sx={styles.requirements} role="list">
              <div role="listitem">• Use two different array positions.</div>
              <div role="listitem">• Return indices, starting from zero.</div>
              <div role="listitem">• Values may be negative or repeated.</div>
            </div>

            <div role="heading" aria-level={2} sx={styles.sectionHeading}>Examples</div>
            {problem.examples.map((item, index) => (
              <div sx={styles.example} key={index}>
                <div sx={styles.exampleTitle}>Example {index + 1}</div>
                <div sx={styles.exampleCode}>{`nums = ${item.nums}, target = ${item.target}\noutput = ${item.expected}`}</div>
                <div sx={styles.exampleExplanation}>{item.explanation}</div>
              </div>
            ))}

            <div role="heading" aria-level={2} sx={styles.sectionHeading}>Constraints</div>
            <div sx={styles.requirements} role="list">
              <div role="listitem">• <span sx={styles.inlineCode}>2 ≤ nums.size ≤ 10,000</span></div>
              <div role="listitem">• <span sx={styles.inlineCode}>−10⁹ ≤ nums[i], target ≤ 10⁹</span></div>
            </div>
            <div sx={styles.followUp}>
              <div sx={styles.followUpTitle}>Go a little further</div>
              <div>Can you find the pair in a single pass through the array?</div>
            </div>
          </div>
        </div>

        <div sx={styles.editorPanel} role="region" aria-labelledby="editor-title">
          <div sx={[styles.panelHeading, styles.editorHeader]}>
            <div role="heading" aria-level={2} id="editor-title" sx={styles.editorTitle}>
              <span sx={styles.languageDot} /> Kotlin Editor
            </div>
            <div sx={styles.editorTools}>
              <Control appearance={[styles.toolButton, styles.fontSelect]}
                label={`Editor font size: ${fontSize} pixels. Activate to change size.`}
                title="Cycle editor font size"
                onActivate={() => setFontSize(fontSizes[(fontSizes.indexOf(fontSize) + 1) % fontSizes.length])}>
                {fontSize} px
              </Control>
              <Control appearance={[styles.toolButton, wordWrap && styles.toolActive]}
                label="Word wrap" pressed={wordWrap} title="Toggle word wrap"
                onActivate={() => setWordWrap(!wordWrap)}>
                <Icon name="wrap" />
              </Control>
              <Control appearance={[styles.toolButton, draft.source === problem.starterCode && styles.toolDisabled]}
                label="Reset to starter code" title="Reset to starter code (can be undone in the editor)"
                disabled={draft.source === problem.starterCode}
                onActivate={() => updateSource(problem.starterCode)}>
                <Icon name="reset" />
              </Control>
            </div>
          </div>
          <div sx={styles.fileBar}>
            <Icon name="code" size={14} />
            <span sx={styles.fileName}>{problem.filename}</span>
          </div>
          <div sx={styles.editor}>
            <CodeEditor value={draft.source} fontSize={fontSize} wordWrap={wordWrap}
              onChange={updateSource} onCursorChange={(line, column) => setCursor({line, column})} />
          </div>
          <div sx={styles.editorFooter}>
            <span>Ln {cursor.line}, Col {cursor.column}</span>
            <span>Spaces: 4 <span aria-hidden="true">·</span> UTF-8</span>
          </div>
          <div id="editor-keyboard-help" sx={styles.screenReader}>
            Tab indents code. Press Escape, then Tab to leave the editor. Use your platform’s undo shortcut to undo edits or a reset.
          </div>
        </div>

        <div sx={styles.controls} aria-label="Run and submit your solution">
          <div sx={styles.actionRow}>
            <Control disabled appearance={[styles.action, styles.runButton]} description="execution-note">
              <Icon name="play" size={15} /> Run Tests
            </Control>
            <Control disabled appearance={[styles.action, styles.submitButton]} description="execution-note">
              <Icon name="submit" size={15} /> Submit
            </Control>
          </div>
          <div id="execution-note" sx={styles.executionNote}>
            Run Tests and Submit will be available when code execution is connected.
          </div>
          <div role="status" sx={[styles.saveStatus, !draft.available && styles.saveError]}>
            <Icon name={draft.available ? "check" : "document"} size={14} />
            {draft.available ? "Drafts are saved on this device" : "Browser storage is unavailable. Copy your code before leaving."}
          </div>
        </div>

        <div sx={styles.results} role="region" aria-labelledby="results-title">
          <div sx={[styles.panelHeading, styles.resultsHeading]}>
            <div role="heading" aria-level={2} id="results-title" sx={styles.panelLabel}><Icon name="terminal" /> Test Results</div>
            <span sx={styles.notRun}>Not run</span>
          </div>
          <div sx={styles.resultsBody}>
            <div sx={styles.caseTabs} role="group" aria-label="Example test cases">
              {problem.examples.map((_, index) => (
                <Control key={index} appearance={[styles.caseButton, index === selectedCase && styles.selectedCase]}
                  pressed={index === selectedCase} controls="selected-case" onActivate={() => setSelectedCase(index)}>
                  Case {index + 1}
                </Control>
              ))}
            </div>
            <div id="selected-case" sx={styles.caseData} aria-live="polite" aria-label={`Case ${selectedCase + 1}`}>
              <div sx={styles.dataBlock}>
                <div sx={styles.dataLabel}>Input</div>
                <div sx={styles.dataValue}>{`nums = ${example.nums}\ntarget = ${example.target}`}</div>
              </div>
              <div sx={styles.dataBlock}>
                <div sx={styles.dataLabel}>Expected output</div>
                <div sx={styles.dataValue}>{example.expected}</div>
              </div>
            </div>
            <div sx={styles.resultsHint}><Icon name="clock" size={13} /> Your test results will appear here after a run.</div>
          </div>
        </div>
      </div>
    </div>
  );
}
