import * as stylex from "@stylexjs/stylex";
import type {TagType} from "__generated__/ProblemPageQuery.graphql";
import {groupTagsByType} from "./groupTagsByType";

type Tag = {
  id: string;
  displayName: string;
  type: TagType;
};

type Props = {
  tags: ReadonlyArray<Tag>;
};

const styles = stylex.create({
  groups: {
    display: "flex",
    flexDirection: "column",
    gap: 8
  },
  group: {
    display: "flex",
    alignItems: "baseline",
    flexWrap: "wrap",
    gap: 8
  },
  label: {
    color: "#9da0a8",
    fontSize: 12,
    fontWeight: 600
  },
  list: {
    display: "flex",
    flexWrap: "wrap",
    gap: 6,
    margin: 0,
    padding: 0,
    listStyle: "none"
  },
  tag: {
    maxWidth: "100%",
    padding: "4px 9px",
    borderRadius: 5,
    color: "#b4b8bf",
    backgroundColor: "#2b2d30",
    fontSize: 12,
    lineHeight: 1.4,
    overflowWrap: "anywhere"
  }
});

export default function ProblemTags(props: Props) {
  if (props.tags.length === 0) {
    return null;
  }

  return (
    <div sx={styles.groups}>
      {groupTagsByType(props.tags).map((group) => (
        <div key={group.label} sx={styles.group}>
          <span sx={styles.label}>{group.label}</span>
          <ul sx={styles.list} aria-label={group.label} role="list">
            {group.tags.map((tag) => (
              <li key={tag.id} sx={styles.tag}>{tag.displayName}</li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}
