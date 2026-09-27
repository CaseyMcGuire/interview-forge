import {useId} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {
  ProblemTestDriverField_language$key,
  TestDriverType
} from "__generated__/ProblemTestDriverField_language.graphql";
import ProblemMarkdown from "components/coding/ProblemMarkdown";

type Props = {
  language: ProblemTestDriverField_language$key;
  value: TestDriverType | null;
  disabled: boolean;
  onChange: (type: TestDriverType) => void;
};

const styles = stylex.create({
  field: {
    display: "flex",
    flexDirection: "column",
    gap: 10
  },
  label: {
    fontWeight: 600
  },
  select: {
    width: "100%",
    boxSizing: "border-box",
    border: "1px solid #4e5157",
    borderRadius: 5,
    padding: "10px 12px",
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: "inherit",
    fontSize: 14,
    outlineColor: "#6b9bfa"
  },
  summary: {
    cursor: "pointer",
    color: "#b5ceff",
    fontSize: 13
  },
  documentation: {
    paddingTop: 14,
    fontSize: 13
  },
  unavailable: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  }
});

export default function ProblemTestDriverField(props: Props) {
  const {language, value, disabled, onChange} = props;
  const data = useFragment(graphql`
    fragment ProblemTestDriverField_language on Language {
      testDriverTypes {
        type
        displayName
        documentationMarkdown
      }
    }
  `, language);
  const id = useId();
  const selected = data.testDriverTypes.find((driver) => driver.type === value);

  function changeType(value: string) {
    const driver = data.testDriverTypes.find((option) => option.type === value);
    if (driver) {
      onChange(driver.type);
    }
  }

  return (
    <div sx={styles.field}>
      <label htmlFor={id} sx={styles.label}>Test driver type</label>
      <select
        id={id}
        sx={styles.select}
        value={value ?? ""}
        disabled={disabled || data.testDriverTypes.length === 0}
        onChange={(event) => changeType(event.target.value)}
      >
        {!selected && (
          <option value={value ?? ""} disabled>
            {value === null ? "No driver types available" : "Current driver type"}
          </option>
        )}
        {data.testDriverTypes.map((driver) => (
          <option key={driver.type} value={driver.type}>{driver.displayName}</option>
        ))}
      </select>

      {selected ? (
        <details>
          <summary sx={styles.summary}>How to write this driver</summary>
          <div sx={styles.documentation}>
            <ProblemMarkdown>{selected.documentationMarkdown}</ProblemMarkdown>
          </div>
        </details>
      ) : (
        <p sx={styles.unavailable}>
          {data.testDriverTypes.length === 0
            ? "No test driver types are available for this language."
            : "Usage instructions are unavailable for the current driver type."}
        </p>
      )}
    </div>
  );
}
