import * as stylex from "@stylexjs/stylex";

type Props = {
  errors: readonly string[];
  saved: boolean;
};

const styles = stylex.create({
  error: {
    color: "#f2a6a6",
    lineHeight: 1.6
  },
  status: {
    color: "#9da0a8"
  }
});

export default function ProblemEditFeedback({errors, saved}: Props) {
  return (
    <>
      {errors.length > 0 && (
        <div sx={styles.error} role="alert">
          {errors.map((message, index) => <div key={index}>{message}</div>)}
        </div>
      )}

      {saved && errors.length === 0 && (
        <div sx={styles.status} role="status">Changes saved.</div>
      )}
    </>
  );
}
