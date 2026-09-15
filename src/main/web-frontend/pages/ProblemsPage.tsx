import {Suspense} from "react";
import * as stylex from "@stylexjs/stylex";
import {graphql, useLazyLoadQuery} from "react-relay";
import type {ProblemsPageQuery} from "__generated__/ProblemsPageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemList from "components/problems/ProblemList";

const styles = stylex.create({
  content: {
    maxWidth: 1040,
    margin: "0 auto",
    padding: {
      default: "40px 28px 64px",
      "@media (max-width: 600px)": "24px 16px 40px"
    }
  },
  title: {
    fontSize: 29,
    fontWeight: 650,
    letterSpacing: "-0.9px",
    marginBottom: 10
  },
  description: {
    color: "#9da0a8",
    lineHeight: 1.6,
    marginBottom: 32
  },
  status: {
    padding: "28px 24px",
    borderRadius: 8,
    backgroundColor: "#2b2d30",
    color: "#9da0a8",
    lineHeight: 1.6
  }
});

function ProblemsContent() {
  const data = useLazyLoadQuery<ProblemsPageQuery>(graphql`
    query ProblemsPageQuery($filters: ProblemFilterInput) @throwOnFieldError {
      ...ProblemList_query @arguments(filters: $filters)
    }
  `, {});

  return <ProblemList query={data} />;
}

export default function ProblemsPage() {
  return (
    <PageLayout title="Problems">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Problems</div>
        <div sx={styles.description}>
          Choose a problem and start coding.
        </div>
        <Suspense fallback={<div sx={styles.status} role="status">Loading problems…</div>}>
          <ProblemsContent />
        </Suspense>
      </div>
    </PageLayout>
  );
}
