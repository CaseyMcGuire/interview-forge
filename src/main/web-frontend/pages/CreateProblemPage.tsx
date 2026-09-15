import {Suspense} from "react";
import * as stylex from "@stylexjs/stylex";
import PageLayout from "components/PageLayout";
import ProblemCreationForm from "components/problems/ProblemCreationForm";

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

export default function CreateProblemPage() {
  return (
    <PageLayout title="Create problem">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Create problem</div>
        <div sx={styles.description}>
          Add the statement, starter code for each language, and shared examples. Publishing makes the problem available to everyone.
        </div>
        <Suspense fallback={<div role="status">Loading languages…</div>}>
          <ProblemCreationForm />
        </Suspense>
      </div>
    </PageLayout>
  );
}
