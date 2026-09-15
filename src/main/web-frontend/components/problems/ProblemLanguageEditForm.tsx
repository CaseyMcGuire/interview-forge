import {useId, useState} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemLanguageEditForm_configuration$key} from "__generated__/ProblemLanguageEditForm_configuration.graphql";
import CodeEditor from "components/coding/CodeEditor";
import Control from "components/coding/WorkspaceControl";

type Props = {
  configuration: ProblemLanguageEditForm_configuration$key;
};

const styles = stylex.create({
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 18,
    padding: 20,
    border: "1px solid #393b40",
    borderRadius: 8
  },
  heading: {
    fontWeight: 600
  },
  editor: {
    height: 280,
    overflow: "hidden",
    border: "1px solid #4e5157",
    borderRadius: 5
  },
  help: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  },
  actions: {
    paddingTop: 18,
    borderTop: "1px solid #393b40"
  },
  save: {
    padding: "10px 16px",
    borderRadius: 5,
    width: "fit-content",
    backgroundColor: "#3574f0",
    color: "#ffffff",
    opacity: 0.6
  }
});

export default function ProblemLanguageEditForm({configuration}: Props) {
  const data = useFragment(graphql`
    fragment ProblemLanguageEditForm_configuration on ProblemLanguage {
      id
      starterCode
      language {
        key
        displayName
      }
    }
  `, configuration);

  const [starterCode, setStarterCode] = useState(data.starterCode);
  const id = useId();

  return (
    <div sx={styles.form}>
      <div sx={styles.heading} role="heading" aria-level={3}>
        {data.language.displayName}
      </div>

      <span>Starter code</span>

      <div sx={styles.editor}>
        <CodeEditor
          languageKey={data.language.key}
          label={`${data.language.displayName} starter code`}
          describedBy={`${id}-help`}
          value={starterCode}
          onChange={setStarterCode}
          fontSize={14}
          wordWrap
          onCursorChange={() => {}}
        />
      </div>

      <div id={`${id}-help`} sx={styles.help}>
        Tab indents code. Press Escape, then Tab to leave the editor.
      </div>

      <div sx={styles.actions}>
        <Control appearance={styles.save} disabled>
          Save starter code
        </Control>
      </div>
    </div>
  );
}
