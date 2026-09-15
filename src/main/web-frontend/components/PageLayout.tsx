import {useEffect, type ReactNode} from "react";
import * as stylex from "@stylexjs/stylex";
import WorkspaceHeader from "components/coding/WorkspaceHeader";

type Props = {
  title: string;
  children: ReactNode;
};

const styles = stylex.create({
  page: {
    minHeight: "100dvh",
    backgroundColor: "#1e1f22",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14
  }
});

export default function PageLayout({title, children}: Props) {
  useEffect(() => {
    document.title = `${title} · Interview Forge`;
  }, [title]);

  return (
    <div sx={styles.page}>
      <WorkspaceHeader />
      {children}
    </div>
  );
}
