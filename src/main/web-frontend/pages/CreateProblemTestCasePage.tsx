import {Suspense} from "react";
import {Link} from "react-router";
import {graphql, usePreloadedQuery, type PreloadedQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {CreateProblemTestCasePageQuery} from "__generated__/CreateProblemTestCasePageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemTestCaseCreationForm from "components/problems/ProblemTestCaseCreationForm";
import {AppRoutes} from "routes/AppRoutes";

type Props = {
  queryRef: PreloadedQuery<CreateProblemTestCasePageQuery>;
};

export const createProblemTestCasePageQuery = graphql`
  query CreateProblemTestCasePageQuery($slug: String!) @throwOnFieldError {
    problem(slug: $slug) {
      id
      slug
      title
      ...ProblemTestCaseCreationForm_problem
    }
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
    marginBottom: 24
  },
  sections: {
    display: "flex",
    flexDirection: "column",
    gap: 24
  },
  problemTitle: {
    fontSize: 20,
    fontWeight: 600,
    margin: 0
  },
  link: {
    color: "#6b9bfa",
    width: "fit-content"
  }
});

function CreateProblemTestCaseContent(props: Props) {
  const {problem} = usePreloadedQuery<CreateProblemTestCasePageQuery>(createProblemTestCasePageQuery, props.queryRef);

  if (!problem) {
    return <div role="status">Problem not found.</div>;
  }

  return (
    <div sx={styles.sections}>
      <Link {...stylex.props(styles.link)} to={AppRoutes.ProblemTestCases({slug: problem.slug})}>
        Back to tests
      </Link>

      <h2 sx={styles.problemTitle}>{problem.title}</h2>

      <ProblemTestCaseCreationForm key={problem.id} problem={problem} />
    </div>
  );
}

export default function CreateProblemTestCasePage(props: Props) {
  return (
    <PageLayout title="Add test case">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Add test case</div>

        <Suspense fallback={<div role="status">Loading problem…</div>}>
          <CreateProblemTestCaseContent queryRef={props.queryRef} />
        </Suspense>
      </div>
    </PageLayout>
  );
}

export function CreateProblemTestCasePageError() {
  return (
    <PageLayout title="Add test case">
      <div sx={styles.content} role="main">
        <div sx={styles.sections}>
          <div role="alert">The problem could not be loaded. Reload the page to try again.</div>

          <Link {...stylex.props(styles.link)} to={AppRoutes.Problems()}>Back to problems</Link>
        </div>
      </div>
    </PageLayout>
  );
}
