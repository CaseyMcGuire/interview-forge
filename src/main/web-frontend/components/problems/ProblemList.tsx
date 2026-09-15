import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import {graphql, usePaginationFragment} from "react-relay";
import {useNavigate} from "react-router";
import type {ProblemList_query$key} from "__generated__/ProblemList_query.graphql";
import type {ProblemListPaginationQuery} from "__generated__/ProblemListPaginationQuery.graphql";
import Control from "components/coding/WorkspaceControl";
import {AppRoutes} from "routes/AppRoutes";
import ProblemDifficultyBadge from "./ProblemDifficultyBadge";

type ProblemListProps = {
  query: ProblemList_query$key;
};

const styles = stylex.create({
  list: {
    backgroundColor: "#2b2d30",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#393b40",
    borderRadius: 8,
    overflow: "hidden"
  },
  header: {
    display: "grid",
    gridTemplateColumns: "minmax(0, 1fr) 90px",
    gap: 16,
    padding: "14px 20px",
    color: "#9da0a8",
    fontSize: 12,
    fontWeight: 600,
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40"
  },
  item: {
    borderBottomWidth: {
      default: 1,
      ":last-child": 0
    },
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40"
  },
  link: {
    display: "grid",
    gridTemplateColumns: "minmax(0, 1fr) 90px",
    alignItems: "center",
    gap: 16,
    minHeight: 68,
    padding: "18px 20px",
    cursor: "pointer",
    backgroundColor: {
      default: "transparent",
      ":hover": "#323439"
    },
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: -2
  },
  title: {
    fontWeight: 500,
    lineHeight: 1.5,
    overflowWrap: "anywhere"
  },
  status: {
    padding: "28px 24px",
    color: "#9da0a8",
    lineHeight: 1.6
  },
  pagination: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: 12,
    padding: "16px 20px",
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#393b40"
  },
  loadMore: {
    padding: "8px 16px",
    borderRadius: 5,
    backgroundColor: {
      default: "#393b40",
      ":hover": "#43454a"
    },
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  error: {
    color: "#eb938d",
    fontSize: 12
  }
});

export default function ProblemList({query}: ProblemListProps) {
  const navigate = useNavigate();
  const [paginationError, setPaginationError] = useState<string | null>(null);
  const {data, loadNext, hasNext, isLoadingNext} = usePaginationFragment<
    ProblemListPaginationQuery,
    ProblemList_query$key
  >(graphql`
    fragment ProblemList_query on Query
    @argumentDefinitions(
      first: {type: "Int", defaultValue: 20}
      after: {type: "String"}
      filters: {type: "ProblemFilterInput"}
    )
    @refetchable(queryName: "ProblemListPaginationQuery") {
      problems(first: $first, after: $after, filters: $filters)
      @connection(key: "ProblemList_problems", filters: ["filters"]) {
        edges {
          node {
            id
            slug
            title
            difficulty
          }
        }
      }
    }
  `, query);
  const problems = data.problems.edges.map((edge) => edge.node);

  if (problems.length === 0) {
    return (
      <div sx={styles.list}>
        <div sx={styles.status} role="status">No problems found.</div>
      </div>
    );
  }

  return (
    <div sx={styles.list}>
      <div sx={styles.header} aria-hidden="true">
        <span>Problem</span>
        <span>Difficulty</span>
      </div>
      <div role="list" aria-label="Coding problems">
        {problems.map((problem) => (
          <div sx={styles.item} role="listitem" key={problem.id}>
            <div
              sx={styles.link}
              role="link"
              tabIndex={0}
              onClick={() => navigate(AppRoutes.Problem({slug: problem.slug}))}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  navigate(AppRoutes.Problem({slug: problem.slug}));
                }
              }}
            >
              <span sx={styles.title}>{problem.title}</span>
              <ProblemDifficultyBadge difficulty={problem.difficulty} />
            </div>
          </div>
        ))}
      </div>
      {hasNext && (
        <div sx={styles.pagination}>
          <Control
            appearance={[styles.loadMore, isLoadingNext && styles.disabled]}
            disabled={isLoadingNext}
            onActivate={() => {
              setPaginationError(null);
              loadNext(20, {
                onComplete: (error) => {
                  if (error) {
                    setPaginationError("Could not load more problems. Try again.");
                  }
                }
              });
            }}
          >
            {isLoadingNext ? "Loading…" : "Load more"}
          </Control>
          {paginationError && <div sx={styles.error} role="alert">{paginationError}</div>}
        </div>
      )}
    </div>
  );
}
