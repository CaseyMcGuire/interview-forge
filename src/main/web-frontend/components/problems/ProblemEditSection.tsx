import {useId, type ReactNode} from "react";
import * as stylex from "@stylexjs/stylex";

type Props = {
  title: string;
  children: ReactNode;
};

const styles = stylex.create({
  section: {
    minWidth: 0,
    border: "1px solid #393b40",
    borderRadius: 8,
    backgroundColor: "#232428",
    overflow: "hidden"
  },
  heading: {
    padding: {
      default: "18px 24px",
      "@media (max-width: 600px)": "16px"
    },
    borderBottom: "1px solid #393b40",
    backgroundColor: "#2b2d30",
    fontSize: 18,
    fontWeight: 600
  },
  content: {
    padding: {
      default: 24,
      "@media (max-width: 600px)": 16
    }
  }
});

export default function ProblemEditSection({title, children}: Props) {
  const id = useId();

  return (
    <div sx={styles.section} role="region" aria-labelledby={id}>
      <div id={id} sx={styles.heading} role="heading" aria-level={2}>
        {title}
      </div>

      <div sx={styles.content}>{children}</div>
    </div>
  );
}
