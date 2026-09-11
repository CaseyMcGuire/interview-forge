import * as stylex from "@stylexjs/stylex";

const iconGlyphs = {
  document: "▤", code: "‹›", reset: "↶", wrap: "↵",
  play: "▷", submit: "↑", check: "✓", terminal: "›_", clock: "◷",
};

export default function WorkspaceIcon({name, size = 16}: {name: keyof typeof iconGlyphs; size?: number}) {
  return <span sx={styles.icon(size)} aria-hidden="true">{iconGlyphs[name]}</span>;
}

const styles = stylex.create({
  icon: (size: number) => ({
    fontSize: size,
    width: size,
    height: size,
    lineHeight: 1,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    flexShrink: 0,
    fontFamily: "monospace"
  })
});
