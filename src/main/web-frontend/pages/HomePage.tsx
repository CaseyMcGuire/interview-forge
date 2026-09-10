import * as stylex from "@stylexjs/stylex";
import { graphql, useLazyLoadQuery } from "react-relay";
import type { HomePageQuery } from "../__generated__/HomePageQuery.graphql";

const homePageQuery = graphql`
  query HomePageQuery {
    welcomeMessage
  }
`;

const styles = stylex.create({
  root: {
    color: "rgb(35, 35, 35)",
    padding: "64px 24px",
    textAlign: "center",
  },
  heading: {
    fontSize: 32,
    lineHeight: 1.2,
  },
});

export default function HomePage() {
  const data = useLazyLoadQuery<HomePageQuery>(homePageQuery, {});

  return (
    <main sx={styles.root}>
      <h1 sx={styles.heading}>{data.welcomeMessage}</h1>
    </main>
  );
}
