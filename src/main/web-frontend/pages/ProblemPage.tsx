import {useParams} from "react-router";
import * as stylex from "@stylexjs/stylex";
import {graphql, useLazyLoadQuery} from "react-relay";
import type {ProblemPageQuery} from "__generated__/ProblemPageQuery.graphql";
import CodingWorkspace from "components/coding/CodingWorkspace";

const styles = stylex.create({
  unavailable: {
    minHeight: "100dvh",
    padding: 24,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
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
  `, {slug});

  if (!problem) {
    return <div sx={styles.unavailable} role="main">Problem not found.</div>;
  }

  return <CodingWorkspace key={problem.id} problem={problem} />;
}
