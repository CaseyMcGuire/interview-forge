import * as stylex from "@stylexjs/stylex";

// Outline icons on a 24-unit grid; each renders at the requested size in the current text color.
const iconPaths = {
  code: <><polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" /></>,
  panel: <><rect x="3" y="4" width="18" height="16" rx="2" /><line x1="9" y1="4" x2="9" y2="20" /></>,
  play: <polygon points="6 4 20 12 6 20 6 4" fill="currentColor" stroke="none" />,
  submit: <><line x1="12" y1="19" x2="12" y2="5" /><polyline points="5 12 12 5 19 12" /></>,
  plus: <><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></>,
  chevronDown: <polyline points="6 9 12 15 18 9" />,
  chevronUp: <polyline points="6 15 12 9 18 15" />,
  wrap: <>
    <line x1="3" y1="6" x2="21" y2="6" />
    <path d="M3 12h12a3 3 0 0 1 0 6h-2" />
    <polyline points="15 16 13 18 15 20" />
    <line x1="3" y1="18" x2="8" y2="18" />
  </>,
  reset: <><polyline points="1 4 1 10 7 10" /><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" /></>,
  check: <polyline points="20 6 9 17 4 12" />,
  close: <><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></>,
  edit: <><path d="m16 3 5 5-12 12-6 1 1-6Z" /><line x1="13" y1="6" x2="18" y2="11" /></>,
  passed: <><circle cx="12" cy="12" r="10" /><polyline points="16 9 10.5 15 8 12.5" /></>,
  failed: <>
    <circle cx="12" cy="12" r="10" />
    <line x1="15" y1="9" x2="9" y2="15" />
    <line x1="9" y1="9" x2="15" y2="15" />
  </>
};

const styles = stylex.create({
  icon: (size: number) => ({
    width: size,
    height: size,
    display: "block",
    flexShrink: 0
  })
});

type Props = {
  name: keyof typeof iconPaths;
  size?: number;
  strokeWidth?: number;
};

export default function WorkspaceIcon({name, size = 16, strokeWidth = 1.75}: Props) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      sx={styles.icon(size)}
    >
      {iconPaths[name]}
    </svg>
  );
}
