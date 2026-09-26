import * as stylex from "@stylexjs/stylex";
import {Link} from "react-router";
import {AppRoutes} from "routes/AppRoutes";

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
  },
  link: {
    color: "#6b9bfa"
  }
});

export default function HomePage() {
  return (
    <div sx={styles.page} role="main">
      <div sx={styles.title} role="heading" aria-level={1}>Problems</div>
      <Link sx={styles.link} to={AppRoutes.Problems()}>Browse problems</Link>
    </div>
  );
}
