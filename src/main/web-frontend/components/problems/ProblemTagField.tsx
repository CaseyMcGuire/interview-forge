import {useState} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemTagField_query$key} from "__generated__/ProblemTagField_query.graphql";
import ProblemTagSelector from "./ProblemTagSelector";
import TagCreationDialog from "./TagCreationDialog";

type Props = {
  query: ProblemTagField_query$key;
  selectedTagIds: ReadonlyArray<string>;
  disabled: boolean;
  onChange: (tagIds: string[]) => void;
};

const styles = stylex.create({
  field: {
    display: "flex",
    flexDirection: "column",
    gap: 12
  },
  create: {
    width: "fit-content",
    padding: "8px 14px",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: "inherit",
    fontSize: 13,
    cursor: "pointer",
    outlineColor: "#6b9bfa",
    outlineOffset: 2
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

export default function ProblemTagField(props: Props) {
  const data = useFragment(graphql`
    fragment ProblemTagField_query on Query {
      tags {
        id
        slug
        displayName
      }
    }
  `, props.query);
  const [creatingTag, setCreatingTag] = useState(false);

  function selectCreatedTag(tagId: string) {
    props.onChange([...props.selectedTagIds, tagId]);
    setCreatingTag(false);
  }

  return (
    <div sx={styles.field}>
      <ProblemTagSelector
        tags={data.tags}
        selectedTagIds={props.selectedTagIds}
        disabled={props.disabled}
        onChange={props.onChange}
      />
      <button
        type="button"
        sx={[styles.create, props.disabled && styles.disabled]}
        disabled={props.disabled}
        onClick={() => setCreatingTag(true)}
      >
        Create tag
      </button>
      {creatingTag && (
        <TagCreationDialog onCreated={selectCreatedTag} onClose={() => setCreatingTag(false)} />
      )}
    </div>
  );
}
