import {Suspense} from "react";
import {Link, useParams} from "react-router";
import {graphql, useLazyLoadQuery} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {EditProblemPageQuery} from "__generated__/EditProblemPageQuery.graphql";
import PageLayout from "components/PageLayout";
import ProblemDetailsForm from "components/problems/ProblemDetailsForm";
import ProblemLanguageEditForm from "components/problems/ProblemLanguageEditForm";
import ProblemExampleEditForm from "components/problems/ProblemExampleEditForm";
import ProblemHiddenTestCaseCreationForm from "components/problems/ProblemHiddenTestCaseCreationForm";
import ProblemEditSection from "components/problems/ProblemEditSection";
import {AppRoutes} from "routes/AppRoutes";

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
  },
  sections: {
    display: "flex",
    flexDirection: "column",
    gap: 32
  },
  entries: {
    display: "flex",
    flexDirection: "column",
    gap: 16
  },
  link: {
    color: "#6b9bfa",
    width: "fit-content"
  }
});

function EditProblemContent({slug}: {slug: string}) {
  const {problem} = useLazyLoadQuery<EditProblemPageQuery>(graphql`
    query EditProblemPageQuery($slug: String!) @throwOnFieldError {
      problem(slug: $slug) {
        id
        slug
        ...ProblemDetailsForm_problem
        languageConfigurations {
          id
          ...ProblemLanguageEditForm_configuration
        }
        examples {
          id
          ...ProblemExampleEditForm_example
        }
      }
    }
  `, {slug}, {fetchPolicy: "network-only"});

  if (!problem) {
    return <div role="status">Problem not found.</div>;
  }

  return (
    <div sx={styles.sections}>
      <Link sx={styles.link} to={AppRoutes.Problem({slug: problem.slug})}>
        View problem
      </Link>

      <ProblemEditSection title="Problem details">
        <ProblemDetailsForm key={problem.id} problem={problem} />
      </ProblemEditSection>

      <ProblemEditSection title="Language configurations">
        <div sx={styles.entries}>
          {problem.languageConfigurations.map((configuration) => (
            <ProblemLanguageEditForm key={configuration.id} configuration={configuration} />
          ))}

          {problem.languageConfigurations.length === 0 && (
            <div>No enabled language configurations.</div>
          )}
        </div>
      </ProblemEditSection>

      <ProblemEditSection title="Public examples">
        <div sx={styles.entries}>
          {problem.examples.map((example, index) => (
            <ProblemExampleEditForm key={example.id} example={example} index={index} />
          ))}

          {problem.examples.length === 0 && <div>No public examples.</div>}
        </div>
      </ProblemEditSection>

      <ProblemEditSection title="Hidden test cases">
        <ProblemHiddenTestCaseCreationForm key={problem.id} problemId={problem.id} />
      </ProblemEditSection>
    </div>
  );
}

export default function EditProblemPage() {
  const {slug} = useParams<"slug">();

  return (
    <PageLayout title="Edit problem">
      <div sx={styles.content} role="main">
        <div sx={styles.title} role="heading" aria-level={1}>Edit problem</div>

        <div sx={styles.description}>
          Edit the problem details, starter code, and public examples, or add hidden test cases. Save each section separately; saved changes take effect immediately.
        </div>

        <Suspense fallback={<div role="status">Loading problem…</div>}>
          {slug ? <EditProblemContent key={slug} slug={slug} /> : <div>Problem not found.</div>}
        </Suspense>
      </div>
    </PageLayout>
  );
}

export function EditProblemPageError() {
  return (
    <PageLayout title="Edit problem">
      <div sx={styles.content} role="main">
        <div sx={styles.description} role="alert">
          The problem could not be loaded. Reload the page to try again.
        </div>

        <Link sx={styles.link} to={AppRoutes.Problems()}>Back to problems</Link>
      </div>
    </PageLayout>
  );
}
