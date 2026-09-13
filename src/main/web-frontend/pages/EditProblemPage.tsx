import {Suspense} from "react";
import {Navigate, useParams} from "react-router";
import {graphql, useLazyLoadQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {EditProblemPageQuery} from "__generated__/EditProblemPageQuery.graphql";
import WorkspaceHeader from "components/coding/WorkspaceHeader";
import ProblemEditForm from "components/problems/ProblemEditForm";
import useDocumentTitle from "hooks/useDocumentTitle";
import {AppRoutes} from "routes/AppRoutes";

const styles = stylex.create({
  page: {
    minHeight: "100dvh",
    backgroundColor: "#1e1f22",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14
  },
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

function EditProblemContent({slug}: {slug: string}) {
  const {problem} = useLazyLoadQuery<EditProblemPageQuery>(graphql`
    query EditProblemPageQuery($slug: String!) @throwOnFieldError {
      problem(slug: $slug) {
        id
        slug
        title
        statementMarkdown
        difficulty
        languageConfigurations {
          id
          starterCode
          solutionFilename
          language {
            id
            key
            displayName
          }
        }
        examples {
          id
          position
          inputJson
          expectedOutputJson
          explanationMarkdown
        }
      }
    }
  `, {slug}, {fetchPolicy: "network-only"});

  if (!problem) {
    return <Navigate to={AppRoutes.Problems()} replace />;
  }

  return <ProblemEditForm key={problem.id} problem={problem} />;
}

export default function EditProblemPage() {
  const {slug} = useParams<"slug">();
  useDocumentTitle("Edit problem · Interview Forge");

  if (!slug) {
    return <Navigate to={AppRoutes.Problems()} replace />;
  }

  return (
    <div sx={styles.page}>
      <WorkspaceHeader />
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Edit problem</div>
        <div sx={styles.description}>
          Update the statement, starter code, and public examples. Saved changes are published immediately.
        </div>
        <Suspense fallback={<div role="status">Loading problem…</div>}>
          <EditProblemContent key={slug} slug={slug} />
        </Suspense>
      </div>
    </div>
  );
}
