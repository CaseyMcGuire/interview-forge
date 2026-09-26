import {useId} from "react";
import * as stylex from "@stylexjs/stylex";
import type {TagType} from "__generated__/ProblemTagField_query.graphql";
import {groupTagsByType} from "./groupTagsByType";

type TagOption = {
  id: string;
  slug: string;
  displayName: string;
  type: TagType;
};

type Props = {
  tags: ReadonlyArray<TagOption>;
  selectedTagIds: ReadonlyArray<string>;
  disabled?: boolean;
  onChange: (tagIds: string[]) => void;
};

const styles = stylex.create({
  field: {
    borderWidth: 0,
    margin: 0,
    padding: 0,
    minWidth: 0
  },
  legend: {
    padding: 0,
    marginBottom: 8,
    fontWeight: 600
  },
  description: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  },
  group: {
    borderWidth: 0,
    margin: "16px 0 0",
    padding: 0,
    minWidth: 0
  },
  groupLegend: {
    padding: 0,
    color: "#9da0a8",
    fontSize: 13,
    fontWeight: 600
  },
  options: {
    display: "grid",
    gridTemplateColumns: {
      default: "repeat(2, minmax(0, 1fr))",
      "@media (max-width: 600px)": "minmax(0, 1fr)"
    },
    gap: 8,
    marginTop: 8
  },
  option: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    padding: "10px 12px",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    cursor: "pointer"
  },
  selected: {
    backgroundColor: "#253754",
    borderColor: "#6b9bfa"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  checkbox: {
    width: 16,
    height: 16,
    flexShrink: 0,
    accentColor: "#6b9bfa",
    cursor: "inherit",
    outlineColor: "#6b9bfa",
    outlineOffset: 3
  },
  label: {
    display: "flex",
    flexDirection: "column",
    minWidth: 0,
    overflowWrap: "anywhere"
  },
  slug: {
    color: "#9da0a8",
    fontSize: 12
  }
});

export default function ProblemTagSelector(props: Props) {
  const id = useId();

  function setTagSelected(tagId: string, selected: boolean) {
    const tagIds = selected
      ? [...props.selectedTagIds, tagId]
      : props.selectedTagIds.filter((id) => id !== tagId);

    props.onChange(tagIds);
  }

  return (
    <fieldset sx={styles.field} disabled={props.disabled} aria-describedby={`${id}-hint`}>
      <legend sx={styles.legend}>Tags</legend>
      <p id={`${id}-hint`} sx={styles.description}>
        {props.tags.length === 0
          ? "No tags available."
          : "Select the topics and companies associated with this problem."}
      </p>

      {groupTagsByType(props.tags).map((group) => (
        <fieldset key={group.label} sx={styles.group}>
          <legend sx={styles.groupLegend}>{group.label}</legend>
          <div sx={styles.options}>
            {group.tags.map((tag) => {
              const selected = props.selectedTagIds.includes(tag.id);

              return (
                <label
                  key={tag.id}
                  sx={[styles.option, selected && styles.selected, props.disabled && styles.disabled]}
                >
                  <input
                    sx={styles.checkbox}
                    type="checkbox"
                    checked={selected}
                    onChange={(event) => setTagSelected(tag.id, event.target.checked)}
                  />
                  <span sx={styles.label}>
                    <span>{tag.displayName}</span>
                    <span sx={styles.slug}>{tag.slug}</span>
                  </span>
                </label>
              );
            })}
          </div>
        </fieldset>
      ))}
    </fieldset>
  );
}
