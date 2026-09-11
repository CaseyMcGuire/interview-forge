import * as stylex from "@stylexjs/stylex";
import type {ExampleTestCase} from "./codingProblemTypes";
import Icon from "./WorkspaceIcon";

type ProblemPanelProps = {
  title: string;
  examples: readonly ExampleTestCase[];
};

export default function ProblemPanel({title, examples}: ProblemPanelProps) {
  return (
    <div sx={styles.problem} role="region" aria-labelledby="problem-title">
      <div sx={styles.panelHeading}>
        <span sx={styles.panelLabel}><Icon name="document" /> Problem</span>
        <span sx={styles.muted}>Sample problem</span>
      </div>
      <div sx={styles.problemBody} tabIndex={0} aria-label="Problem description">
        <div sx={styles.eyebrow}>Arrays &amp; hashing</div>
        <div role="heading" aria-level={1} id="problem-title" sx={styles.title}>{title}</div>
        <div sx={styles.badges}>
          <span sx={[styles.badge, styles.easyBadge]}>Easy</span>
          <span sx={styles.badge}>Array</span>
          <span sx={styles.badge}>Hash map</span>
        </div>
        <div sx={styles.paragraph}>
          Find two distinct positions in <span sx={styles.inlineCode}>nums</span> whose values
          sum to <span sx={styles.inlineCode}>target</span>.
        </div>
        <div sx={styles.paragraph}>
          Return those positions as a two-element array. The order of the positions doesn’t matter.
          Each input has exactly one matching pair.
        </div>

        <div role="heading" aria-level={2} sx={styles.sectionHeading}>Requirements</div>
        <div sx={styles.requirements} role="list">
          <div role="listitem">• Use two different array positions.</div>
          <div role="listitem">• Return indices, starting from zero.</div>
          <div role="listitem">• Values may be negative or repeated.</div>
        </div>

        <div role="heading" aria-level={2} sx={styles.sectionHeading}>Examples</div>
        {examples.map((item, index) => (
          <div sx={styles.example} key={index}>
            <div sx={styles.exampleTitle}>Example {index + 1}</div>
            <div sx={styles.exampleCode}>{`nums = ${item.nums}, target = ${item.target}\noutput = ${item.expected}`}</div>
            <div sx={styles.exampleExplanation}>{item.explanation}</div>
          </div>
        ))}

        <div role="heading" aria-level={2} sx={styles.sectionHeading}>Constraints</div>
        <div sx={styles.requirements} role="list">
          <div role="listitem">• <span sx={styles.inlineCode}>2 ≤ nums.size ≤ 10,000</span></div>
          <div role="listitem">• <span sx={styles.inlineCode}>−10⁹ ≤ nums[i], target ≤ 10⁹</span></div>
        </div>
        <div sx={styles.followUp}>
          <div sx={styles.followUpTitle}>Go a little further</div>
          <div>Can you find the pair in a single pass through the array?</div>
        </div>
      </div>
    </div>
  );
}

const styles = stylex.create({
  problem: {
    minHeight: 0,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
    borderRight: {
      default: "1px solid #dce1d9",
      "@media (max-width: 800px)": "none"
    }
  },
  panelHeading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    minHeight: 49,
    padding: "0 24px",
    borderBottom: "1px solid #e8ece5",
    flexShrink: 0
  },
  panelLabel: {
    display: "flex",
    alignItems: "center",
    gap: 9,
    fontSize: 12,
    fontWeight: 600
  },
  muted: {
    color: "#748077",
    fontSize: 12
  },
  problemBody: {
    overflowY: "auto",
    padding: {
      default: "28px 32px 32px",
      "@media (max-width: 1100px)": "24px"
    },
    scrollbarWidth: "thin",
    scrollbarColor: "#d5ddd2 transparent"
  },
  eyebrow: {
    fontSize: 10,
    letterSpacing: "1.6px",
    fontWeight: 650,
    color: "#82907f",
    textTransform: "uppercase"
  },
  title: {
    fontSize: 29,
    lineHeight: 1.25,
    letterSpacing: "-0.9px",
    fontWeight: 650,
    marginTop: 8,
    marginBottom: 14
  },
  badges: {
    display: "flex",
    alignItems: "center",
    gap: 7,
    flexWrap: "wrap",
    marginBottom: 26
  },
  badge: {
    fontSize: 11,
    lineHeight: 1.4,
    padding: "4px 9px",
    borderRadius: 5,
    color: "#67736b",
    backgroundColor: "#f1f3ee"
  },
  easyBadge: {
    color: "#2a7051",
    backgroundColor: "#eaf3e9",
    fontWeight: 600
  },
  paragraph: {
    marginBottom: 14,
    color: "#536158",
    fontSize: 14,
    lineHeight: 1.8
  },
  inlineCode: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: "0.9em",
    backgroundColor: "#f1f3ee",
    padding: "2px 5px",
    borderRadius: 4,
    color: "#435546"
  },
  sectionHeading: {
    fontSize: 13,
    fontWeight: 650,
    marginTop: 25,
    marginBottom: 12,
    color: "#34483a"
  },
  requirements: {
    color: "#536158",
    fontSize: 13,
    lineHeight: 2
  },
  example: {
    borderLeft: "2px solid #dce7d5",
    padding: "1px 0 1px 16px",
    marginBottom: 23
  },
  exampleTitle: {
    fontSize: 11,
    color: "#7c887b",
    fontWeight: 600,
    marginBottom: 7
  },
  exampleCode: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.9,
    color: "#435346",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  exampleExplanation: {
    fontSize: 12,
    color: "#7a847b",
    lineHeight: 1.7,
    marginTop: 7
  },
  followUp: {
    marginTop: 26,
    padding: "15px 17px",
    backgroundColor: "#f5f7f2",
    borderRadius: 7,
    fontSize: 12,
    color: "#61745e"
  },
  followUpTitle: {
    fontWeight: 650,
    marginBottom: 3
  }
});
