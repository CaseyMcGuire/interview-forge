import {useEffect, useId, useState, type ReactNode} from "react";
import * as stylex from "@stylexjs/stylex";
import type {StyleXStyles} from "@stylexjs/stylex";

type WorkspaceControlProps = {
  children: ReactNode;
  onActivate?: () => void;
  disabled?: boolean;
  label?: string;
  description?: string;
  pressed?: boolean;
  expanded?: boolean;
  controls?: string;
  appearance: StyleXStyles;
  title?: string;
  tooltip?: string;
  tooltipAlign?: "start" | "end";
};

const styles = stylex.create({
  tooltipAnchor: {
    position: "relative",
    display: "inline-flex"
  },
  tooltip: {
    position: "absolute",
    top: "100%",
    right: 0,
    paddingTop: 8,
    width: "max-content",
    maxWidth: 240,
    zIndex: 10
  },
  tooltipStart: {
    right: "auto",
    left: 0
  },
  tooltipText: {
    display: "block",
    padding: "6px 9px",
    borderRadius: 5,
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    boxShadow: "0 3px 8px rgba(0, 0, 0, 0.25)",
    fontSize: 12,
    lineHeight: 1.4,
    fontWeight: 400,
    whiteSpace: "normal"
  }
});

export default function WorkspaceControl(props: WorkspaceControlProps) {
  const {children, onActivate, disabled, label, description, pressed, expanded, controls, appearance, title} = props;
  const {tooltip, tooltipAlign = "end"} = props;
  const tooltipId = useId();
  const [tooltipOpen, setTooltipOpen] = useState(false);
  const describedBy = [description, tooltip && tooltipOpen ? tooltipId : undefined].filter(Boolean).join(" ") || undefined;

  useEffect(() => {
    if (!tooltipOpen) {
      return;
    }

    function dismissTooltip(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setTooltipOpen(false);
      }
    }

    window.addEventListener("keydown", dismissTooltip);
    return () => window.removeEventListener("keydown", dismissTooltip);
  }, [tooltipOpen]);

  const control = (
    <div
      role="button"
      tabIndex={disabled ? -1 : 0}
      sx={appearance}
      aria-label={label}
      aria-disabled={disabled}
      aria-describedby={describedBy}
      aria-pressed={pressed}
      aria-expanded={expanded}
      aria-controls={controls}
      title={tooltip ? undefined : title}
      onClick={disabled ? undefined : onActivate}
      onKeyDown={(event) => {
        if (!disabled && (event.key === "Enter" || event.key === " ")) {
          event.preventDefault();
          onActivate?.();
        }
      }}
    >
      {children}
    </div>
  );

  if (!tooltip) {
    return control;
  }

  return (
    <span
      sx={styles.tooltipAnchor}
      onMouseEnter={() => setTooltipOpen(true)}
      onMouseLeave={(event) => {
        if (!event.currentTarget.contains(document.activeElement)) {
          setTooltipOpen(false);
        }
      }}
      onFocus={() => setTooltipOpen(true)}
      onBlur={(event) => {
        if (!event.currentTarget.matches(":hover")) {
          setTooltipOpen(false);
        }
      }}
    >
      {control}
      {tooltipOpen && (
        <span id={tooltipId} role="tooltip" sx={[styles.tooltip, tooltipAlign === "start" && styles.tooltipStart]}>
          <span sx={styles.tooltipText}>{tooltip}</span>
        </span>
      )}
    </span>
  );
}
