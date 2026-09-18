import {useParams} from "react-router";
import * as stylex from "@stylexjs/stylex";
import {graphql, useLazyLoadQuery} from "react-relay";
import type {ProblemPageQuery} from "__generated__/ProblemPageQuery.graphql";
import CodingWorkspace from "components/coding/CodingWorkspace";
import WorkspaceHeader from "components/coding/WorkspaceHeader";
import usePageTitle from "hooks/usePageTitle";

const styles = stylex.create({
  page: {
    height: {
      default: "100dvh",
      "@media (max-width: 800px)": "auto"
    },
    minHeight: 640,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#2b2d30",
    colorScheme: "dark",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14,
    lineHeight: 1.6
  },
  unavailable: {
    padding: 24
  }
});

export default function ProblemPage() {
  const {slug} = useParams<"slug">();
  if (!slug) throw new Error("ProblemPage requires a slug.");

  const {problem} = useLazyLoadQuery<ProblemPageQuery>(graphql`
    query ProblemPageQuery($slug: String!) @throwOnFieldError {
      problem(slug: $slug) {
        id
        slug
        title
        statementMarkdown
        difficulty
        languageConfigurations {
          id
          starterCode
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
  `, {slug});

  usePageTitle(problem?.title ?? "Problem not found");

  return (
    <div sx={styles.page}>
      <WorkspaceHeader />
      {problem ? (
        <CodingWorkspace key={problem.id} problem={problem} />
      ) : (
        <div sx={styles.unavailable} role="main">Problem not found.</div>
      )}
    </div>
  );
}
