import {Suspense} from "react";
import * as stylex from "@stylexjs/stylex";
import {graphql, usePreloadedQuery, type PreloadedQuery} from "react-relay";
import type {CreateProblemPageQuery} from "__generated__/CreateProblemPageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemCreationForm from "components/problems/ProblemCreationForm";

type Props = {
  queryRef: PreloadedQuery<CreateProblemPageQuery>;
};

export const createProblemPageQuery = graphql`
  query CreateProblemPageQuery @throwOnFieldError {
    ...ProblemCreationForm_query
  }
`;

const styles = stylex.create({
  content: {
    maxWidth: 920,
    margin: "0 auto",
    padding: {
      default: "40px 28px 64px",
      "@media (max-width: 600px)": "24px 16px 40px"
    }
  },
  title: {
    fontSize: 29,
    fontWeight: 650,
    marginBottom: 10
  },
  description: {
    color: "#9da0a8",
    lineHeight: 1.6,
    marginBottom: 32
  }
});

function CreateProblemContent(props: Props) {
  const data = usePreloadedQuery<CreateProblemPageQuery>(createProblemPageQuery, props.queryRef);

  return <ProblemCreationForm query={data} />;
}

export default function CreateProblemPage(props: Props) {
  return (
    <PageLayout title="Create problem">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Create problem</div>
        <div sx={styles.description}>
          Add the statement, starter code for each language, and shared examples. Publishing makes the problem available to everyone.
        </div>
        <Suspense fallback={<div role="status">Loading languages…</div>}>
          <CreateProblemContent queryRef={props.queryRef} />
        </Suspense>
      </div>
    </PageLayout>
  );
}
