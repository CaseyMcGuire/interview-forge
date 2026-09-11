import type {ReactNode} from "react";
import type {StyleXStyles} from "@stylexjs/stylex";

type WorkspaceControlProps = {
  children: ReactNode;
  onActivate?: () => void;
  disabled?: boolean;
  label?: string;
  description?: string;
  pressed?: boolean;
  controls?: string;
  appearance: StyleXStyles;
  title?: string;
};

export default function WorkspaceControl({
  children,
  onActivate,
  disabled,
  label,
  description,
  pressed,
  controls,
  appearance,
  title,
}: WorkspaceControlProps) {
  return (
    <div
      role="button"
      tabIndex={disabled ? -1 : 0}
      sx={appearance}
      aria-label={label}
      aria-disabled={disabled}
      aria-describedby={description}
      aria-pressed={pressed}
      aria-controls={controls}
      title={title}
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
}
