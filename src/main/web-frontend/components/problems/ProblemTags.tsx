import * as stylex from "@stylexjs/stylex";

type Tag = {
  id: string;
  displayName: string;
};

type Props = {
  tags: ReadonlyArray<Tag>;
};

const styles = stylex.create({
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
    <ul sx={styles.list} aria-label="Tags" role="list">
      {props.tags.map((tag) => (
        <li key={tag.id} sx={styles.tag}>{tag.displayName}</li>
      ))}
    </ul>
  );
}
