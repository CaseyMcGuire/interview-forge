import * as stylex from "@stylexjs/stylex";
import {graphql, usePreloadedQuery, type PreloadedQuery} from "react-relay";
import type {ProblemPageQuery} from "__generated__/ProblemPageQuery.graphql";
import CodingWorkspace from "components/coding/CodingWorkspace";
import WorkspaceHeader from "components/coding/WorkspaceHeader";
import usePageTitle from "hooks/usePageTitle";

type Props = {
  queryRef: PreloadedQuery<ProblemPageQuery>;
};

export const problemPageQuery = graphql`
  query ProblemPageQuery($slug: String!) @throwOnFieldError {
    problem(slug: $slug) {
      id
      slug
      title
      statementMarkdown
      difficulty
      canEdit
      tags {
        id
        displayName
        type
      }
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
`;

const styles = stylex.create({
  page: {
    height: {
      default: "100dvh",
      "@media (max-width: 800px)": "auto"
    },
    minHeight: 640,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#1e1f22",
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

export default function ProblemPage(props: Props) {
  const {problem} = usePreloadedQuery<ProblemPageQuery>(problemPageQuery, props.queryRef);

  usePageTitle(problem?.title ?? "Problem not found");

  return (
    <div sx={styles.page}>
      {problem ? (
        <CodingWorkspace key={problem.id} problem={problem} />
      ) : (
        <>
          <WorkspaceHeader />
          <div sx={styles.unavailable} role="main">Problem not found.</div>
        </>
      )}
    </div>
  );
}
