import {Suspense} from "react";
import {Link, useParams} from "react-router";
import {graphql, useLazyLoadQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {CreateProblemHiddenTestCasePageQuery} from "__generated__/CreateProblemHiddenTestCasePageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemHiddenTestCaseCreationForm from "components/problems/ProblemHiddenTestCaseCreationForm";
import {AppRoutes} from "routes/AppRoutes";

type Props = {
  slug: string;
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

function CreateProblemHiddenTestCaseContent(props: Props) {
  const {problem} = useLazyLoadQuery<CreateProblemHiddenTestCasePageQuery>(graphql`
    query CreateProblemHiddenTestCasePageQuery($slug: String!) @throwOnFieldError {
      problem(slug: $slug) {
        id
        slug
        title
        ...ProblemHiddenTestCaseCreationForm_problem
      }
    }
  `, {slug: props.slug}, {fetchPolicy: "network-only"});

  if (!problem) {
    return <div role="status">Problem not found.</div>;
  }

  return (
    <div sx={styles.sections}>
      <Link {...stylex.props(styles.link)} to={AppRoutes.EditProblem({slug: problem.slug})}>
        Back to edit problem
      </Link>

      <h2 sx={styles.problemTitle}>{problem.title}</h2>

      <ProblemHiddenTestCaseCreationForm key={problem.id} problem={problem} />
    </div>
  );
}

export default function CreateProblemHiddenTestCasePage() {
  const {slug} = useParams<"slug">();

  return (
    <PageLayout title="Add hidden test case">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Add hidden test case</div>

        <Suspense fallback={<div role="status">Loading problem…</div>}>
          {slug
            ? <CreateProblemHiddenTestCaseContent key={slug} slug={slug} />
            : <div role="status">Problem not found.</div>}
        </Suspense>
      </div>
    </PageLayout>
  );
}

export function CreateProblemHiddenTestCasePageError() {
  return (
    <PageLayout title="Add hidden test case">
      <div sx={styles.content} role="main">
        <div sx={styles.sections}>
          <div role="alert">The problem could not be loaded. Reload the page to try again.</div>

          <Link {...stylex.props(styles.link)} to={AppRoutes.Problems()}>Back to problems</Link>
        </div>
      </div>
    </PageLayout>
  );
}
