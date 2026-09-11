import * as stylex from "@stylexjs/stylex";
import type {ProblemDifficulty} from "__generated__/ProblemList_query.graphql";

type ProblemDifficultyBadgeProps = {
  difficulty: ProblemDifficulty;
};

const styles = stylex.create({
  badge: {
    justifySelf: "start",
    padding: "4px 9px",
    borderRadius: 5,
    fontSize: 12,
    lineHeight: 1.4,
    color: "#b4b8bf",
    backgroundColor: "#393b40"
  },
  easy: {
    color: "#89cc8e",
    backgroundColor: "#253627"
  },
  medium: {
    color: "#f2c55c",
    backgroundColor: "#413923"
  },
  hard: {
    color: "#eb938d",
    backgroundColor: "#452e30"
  }
});

function getDifficultyLabel(difficulty: ProblemDifficulty): string {
  switch (difficulty) {
    case "EASY":
      return "Easy";
    case "MEDIUM":
      return "Medium";
    case "HARD":
      return "Hard";
    default:
      return "Unknown";
  }
}

export default function ProblemDifficultyBadge({difficulty}: ProblemDifficultyBadgeProps) {
  return (
    <span sx={[
      styles.badge,
      difficulty === "EASY" && styles.easy,
      difficulty === "MEDIUM" && styles.medium,
      difficulty === "HARD" && styles.hard
    ]}>
      {getDifficultyLabel(difficulty)}
    </span>
  );
}
