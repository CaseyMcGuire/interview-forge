import * as stylex from "@stylexjs/stylex";

const styles = stylex.create({
  page: {
    minHeight: "100dvh",
    padding: 24,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14
  },
  title: {
    fontSize: 24,
    fontWeight: 600,
    marginBottom: 8
  }
});

export default function HomePage() {
  return (
    <div sx={styles.page} role="main">
      <div sx={styles.title} role="heading" aria-level={1}>Problems</div>
      <div>Problem index coming soon.</div>
    </div>
  );
}
