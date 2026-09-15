import {useState} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemExampleEditForm_example$key} from "__generated__/ProblemExampleEditForm_example.graphql";
import Control from "components/coding/WorkspaceControl";
import ProblemExampleEditor from "./ProblemExampleEditor";

type Props = {
  example: ProblemExampleEditForm_example$key;
  index: number;
};

const styles = stylex.create({
  actions: {
    display: "flex",
    flexWrap: "wrap",
    gap: 12,
    paddingTop: 18,
    borderTop: "1px solid #393b40"
  },
  control: {
    padding: "10px 16px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    opacity: 0.6
  },
  save: {
    backgroundColor: "#3574f0",
    borderColor: "#3574f0",
    color: "#ffffff"
  },
  delete: {
    color: "#f2a6a6"
  }
});

export default function ProblemExampleEditForm({example, index}: Props) {
  const data = useFragment(graphql`
    fragment ProblemExampleEditForm_example on ProblemExample {
      id
      inputJson
      expectedOutputJson
      explanationMarkdown
    }
  `, example);

  const [draft, setDraft] = useState({
    inputJson: data.inputJson,
    expectedOutputJson: data.expectedOutputJson,
    explanationMarkdown: data.explanationMarkdown
  });

  return (
    <ProblemExampleEditor
      example={draft}
      index={index}
      disabled={false}
      canRemove={false}
      onChange={(updated) => setDraft({
        inputJson: updated.inputJson,
        expectedOutputJson: updated.expectedOutputJson,
        explanationMarkdown: updated.explanationMarkdown ?? null
      })}
    >
      <div sx={styles.actions}>
        <Control appearance={[styles.control, styles.save]} disabled>
          Save example
        </Control>

        <Control appearance={[styles.control, styles.delete]} disabled>
          Delete example
        </Control>
      </div>
    </ProblemExampleEditor>
  );
}
