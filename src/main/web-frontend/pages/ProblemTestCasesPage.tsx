import {Suspense} from "react";
import {Link, useNavigate, useParams} from "react-router";
import {graphql, useLazyLoadQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemTestCasesPageQuery} from "__generated__/ProblemTestCasesPageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemTestCaseList from "components/problems/ProblemTestCaseList";
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

function ProblemTestCasesContent(props: Props) {
  const navigate = useNavigate();
  const {problem} = useLazyLoadQuery<ProblemTestCasesPageQuery>(graphql`
    query ProblemTestCasesPageQuery($slug: String!) @throwOnFieldError {
      problem(slug: $slug) {
        slug
        title
        testCases {
          id
          inputJson
          expectedOutputJson
        }
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

      {problem.testCases == null ? (
        <div role="status">Test cases are unavailable.</div>
      ) : (
        <ProblemTestCaseList
          testCases={problem.testCases}
          onCreate={() => navigate(AppRoutes.CreateProblemTestCase({slug: problem.slug}))}
          onEdit={id => navigate(AppRoutes.EditProblemTestCase({slug: problem.slug, id}))}
        />
      )}
    </div>
  );
}

export default function ProblemTestCasesPage() {
  const {slug} = useParams<"slug">();

  return (
    <PageLayout title="Test cases">
      <main sx={styles.content}>
        <h1 sx={styles.title}>Test cases</h1>

        <Suspense fallback={<div role="status">Loading test cases…</div>}>
          {slug ? <ProblemTestCasesContent key={slug} slug={slug} /> : <div role="status">Problem not found.</div>}
        </Suspense>
      </main>
    </PageLayout>
  );
}

export function ProblemTestCasesPageError() {
  return (
    <PageLayout title="Test cases">
      <main sx={styles.content}>
        <h1 sx={styles.title}>Test cases</h1>
        <div sx={styles.sections}>
          <div role="alert">The test cases could not be loaded. Reload the page to try again.</div>
          <Link {...stylex.props(styles.link)} to={AppRoutes.Problems()}>Back to problems</Link>
        </div>
      </main>
    </PageLayout>
  );
}
