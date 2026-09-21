import {Suspense} from "react";
import {Link, useParams} from "react-router";
import {graphql, useLazyLoadQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {EditProblemTestCasePageQuery} from "__generated__/EditProblemTestCasePageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemTestCaseEditForm from "components/problems/ProblemTestCaseEditForm";
import {AppRoutes} from "routes/AppRoutes";

type Props = {
  slug: string;
  id: string;
};

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
    margin: "0 0 24px"
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

function EditProblemTestCaseContent(props: Props) {
  const {problem} = useLazyLoadQuery<EditProblemTestCasePageQuery>(graphql`
    query EditProblemTestCasePageQuery($slug: String!, $id: ID!) @throwOnFieldError {
      problem(slug: $slug) {
        slug
        title
        ...ProblemTestCaseEditForm_problem
        testCase(id: $id) {
          id
          ...ProblemTestCaseEditForm_testCase
        }
      }
    }
  `, {slug: props.slug, id: props.id}, {fetchPolicy: "network-only"});

  if (!problem) {
    return <div role="status">Problem not found.</div>;
  }

  return (
    <div sx={styles.sections}>
      <Link {...stylex.props(styles.link)} to={AppRoutes.ProblemTestCases({slug: problem.slug})}>
        Back to tests
      </Link>

      <h2 sx={styles.problemTitle}>{problem.title}</h2>

      {problem.testCase ? (
        <ProblemTestCaseEditForm key={problem.testCase.id} problem={problem} testCase={problem.testCase} />
      ) : (
        <div role="status">Test case not found.</div>
      )}
    </div>
  );
}

export default function EditProblemTestCasePage() {
  const {slug, id} = useParams<"slug" | "id">();

  return (
    <PageLayout title="Edit test case">
      <main sx={styles.content}>
        <h1 sx={styles.title}>Edit test case</h1>

        <Suspense fallback={<div role="status">Loading test case…</div>}>
          {slug && id
            ? <EditProblemTestCaseContent key={`${slug}:${id}`} slug={slug} id={id} />
            : <div role="status">Test case not found.</div>}
        </Suspense>
      </main>
    </PageLayout>
  );
}

export function EditProblemTestCasePageError() {
  return (
    <PageLayout title="Edit test case">
      <main sx={styles.content}>
        <h1 sx={styles.title}>Edit test case</h1>
        <div sx={styles.sections}>
          <div role="alert">The test case could not be loaded. Reload the page to try again.</div>
          <Link {...stylex.props(styles.link)} to={AppRoutes.Problems()}>Back to problems</Link>
        </div>
      </main>
    </PageLayout>
  );
}
