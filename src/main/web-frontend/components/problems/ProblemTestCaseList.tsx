import * as stylex from "@stylexjs/stylex";
import Control from "components/coding/WorkspaceControl";

type TestCase = {
  id: string;
  inputJson: string;
  expectedOutputJson: string;
};

type Props = {
  testCases: readonly TestCase[];
  onCreate: () => void;
  onEdit: (id: string) => void;
};

const styles = stylex.create({
  section: {
    display: "flex",
    flexDirection: "column",
    gap: 20
  },
  heading: {
    display: "flex",
    flexWrap: "wrap",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12
  },
  count: {
    color: "#9da0a8",
    fontSize: 14
  },
  control: {
    padding: "8px 14px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    cursor: "pointer",
    width: "fit-content"
  },
  create: {
    backgroundColor: "#3574f0",
    borderColor: "#3574f0",
    color: "#ffffff"
  },
  list: {
    margin: 0,
    padding: 0,
    listStyle: "none",
    border: "1px solid #393b40",
    borderRadius: 8,
    overflow: "hidden"
  },
  item: {
    padding: 20,
    display: "flex",
    flexDirection: "column",
    gap: 16,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40",
    borderBottomWidth: {
      default: 1,
      ":last-child": 0
    }
  },
  title: {
    fontSize: 15,
    fontWeight: 600,
    margin: 0
  },
  columns: {
    display: "grid",
    gridTemplateColumns: {
      default: "minmax(0, 1fr) minmax(0, 1fr)",
      "@media (max-width: 600px)": "minmax(0, 1fr)"
    },
    gap: 16
  },
  label: {
    display: "block",
    color: "#9da0a8",
    fontSize: 12,
    marginBottom: 6
  },
  preview: {
    margin: 0,
    fontFamily: "monospace",
    fontSize: 13,
    lineHeight: 1.5,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere",
    display: "-webkit-box",
    WebkitBoxOrient: "vertical",
    WebkitLineClamp: 3,
    overflow: "hidden"
  },
  output: {
    userSelect: "none"
  },
  empty: {
    border: "1px solid #393b40",
    borderRadius: 8,
    padding: 24,
    color: "#9da0a8",
    lineHeight: 1.6
  }
});

export default function ProblemTestCaseList(props: Props) {
  const count = props.testCases.length;

  return (
    <div sx={styles.section}>
      <div sx={styles.heading}>
        <span sx={styles.count}>{count} {count === 1 ? "test case" : "test cases"}</span>

        <Control appearance={[styles.control, styles.create]} onActivate={props.onCreate}>
          Add test case
        </Control>
      </div>

      {count === 0 ? (
        <div sx={styles.empty} role="status">No test cases yet. Add a test case to get started.</div>
      ) : (
        <ol sx={styles.list} aria-label="Test cases">
          {props.testCases.map((testCase, index) => (
            <li sx={styles.item} key={testCase.id}>
              <div sx={styles.heading}>
                <h3 sx={styles.title}>Test {index + 1}</h3>

                <Control
                  appearance={styles.control}
                  label={`Edit test ${index + 1}`}
                  onActivate={() => props.onEdit(testCase.id)}
                >
                  Edit
                </Control>
              </div>

              <div sx={styles.columns}>
                <div>
                  <span sx={styles.label}>Input</span>
                  <pre sx={styles.preview}>{testCase.inputJson}</pre>
                </div>

                <div>
                  <span sx={styles.label}>Expected output</span>
                  <pre sx={[styles.preview, styles.output]}>{testCase.expectedOutputJson}</pre>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
