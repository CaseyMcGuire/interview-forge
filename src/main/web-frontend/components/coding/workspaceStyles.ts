import * as stylex from "@stylexjs/stylex";

export const styles = stylex.create({
  page: {
    height: {
      default: "100dvh",
      "@media (max-width: 800px)": "auto"
    },
    minHeight: 640,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#f3f4f1",
    color: "#25332e",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14,
    lineHeight: 1.6
  },
  header: {
    height: 72,
    flexShrink: 0,
    padding: {
      default: "0 28px",
      "@media (max-width: 600px)": "0 16px"
    },
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 16
  },
  brand: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    fontWeight: 650,
    fontSize: 17,
    letterSpacing: "-0.5px"
  },
  brandMark: {
    display: "grid",
    placeItems: "center",
    width: 30,
    height: 30,
    borderRadius: 8,
    backgroundColor: "#244f3e",
    color: "#e1f2df",
    fontFamily: "monospace",
    fontSize: 16
  },
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
  }),
  headerContext: {
    display: "flex",
    alignItems: "center",
    gap: 18,
    fontSize: 12,
    color: "#68756d"
  },
  contextLabel: {
    display: {
      default: "inline",
      "@media (max-width: 600px)": "none"
    }
  },
  previewBadge: {
    padding: "4px 10px",
    border: "1px solid #d7ddd5",
    borderRadius: 6,
    color: "#627060",
    fontSize: 11,
    fontWeight: 600
  },
  workspace: {
    display: "grid",
    gridTemplateColumns: {
      default: "minmax(0, 0.43fr) minmax(0, 0.57fr)",
      "@media (max-width: 800px)": "minmax(0, 1fr)"
    },
    gridTemplateRows: {
      default: "minmax(0, 1fr) 254px",
      "@media (max-width: 800px)": "auto 450px auto auto"
    },
    flex: 1,
    minHeight: 0,
    margin: {
      default: "0 20px 20px",
      "@media (max-width: 600px)": "0 8px 8px"
    },
    border: "1px solid #dce1d9",
    borderRadius: 12,
    overflow: "hidden",
    backgroundColor: "#ffffff",
    boxShadow: "0 4px 20px #25332e05"
  },
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
  },
  editorPanel: {
    display: "flex",
    flexDirection: "column",
    minWidth: 0,
    minHeight: 0,
    backgroundColor: "#1d2428",
    color: "#dce3e6"
  },
  editorHeader: {
    borderBottom: "1px solid #344047",
    padding: "0 20px",
    flexWrap: "wrap",
    rowGap: 4
  },
  editorTitle: {
    fontWeight: 500,
    color: "#d0dbd4",
    fontSize: 12,
    display: "flex",
    alignItems: "center",
    gap: 8
  },
  languageDot: {
    width: 7,
    height: 7,
    borderRadius: "50%",
    backgroundColor: "#b79bdd"
  },
  editorTools: {
    display: "flex",
    alignItems: "center",
    gap: 6
  },
  toolButton: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    minHeight: 30,
    minWidth: 30,
    padding: "4px 7px",
    backgroundColor: "transparent",
    border: "1px solid transparent",
    color: {
      default: "#a5b2b9",
      ":hover": "#edf4ef"
    },
    borderRadius: 5,
    cursor: "pointer",
    fontSize: 11,
    outlineOffset: 2,
    userSelect: "none"
  },
  toolActive: {
    backgroundColor: "#34443e",
    color: "#c4dec9",
    borderColor: "#4a6155"
  },
  toolDisabled: {
    opacity: 0.35,
    cursor: "default"
  },
  fontSelect: {
    fontFamily: "inherit",
    fontSize: 11,
    backgroundColor: "#263036",
    color: "#bac6ca",
    border: "1px solid #425159",
    padding: "4px 3px",
    borderRadius: 4,
    cursor: "pointer"
  },
  fileBar: {
    height: 41,
    flexShrink: 0,
    padding: "0 24px",
    display: "flex",
    alignItems: "center",
    gap: 8,
    color: "#91a097",
    fontSize: 11,
    borderBottom: "1px solid #273236"
  },
  fileName: {
    color: "#c5cfc7",
    fontFamily: '"SFMono-Regular", Consolas, monospace'
  },
  editor: {
    flex: 1,
    minHeight: 0,
    minWidth: 0
  },
  editorFooter: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12,
    flexShrink: 0,
    minHeight: 30,
    padding: "0 20px",
    borderTop: "1px solid #344047",
    fontSize: 10,
    color: "#8c9ca2"
  },
  controls: {
    borderTop: "1px solid #dce1d9",
    borderRight: {
      default: "1px solid #dce1d9",
      "@media (max-width: 800px)": "none"
    },
    backgroundColor: "#fafbf8",
    padding: "25px 28px",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    gap: 17
  },
  actionRow: {
    display: "flex",
    gap: 10,
    flexWrap: "wrap"
  },
  action: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    padding: "10px 20px",
    borderRadius: 7,
    fontWeight: 600,
    fontFamily: "inherit",
    fontSize: 13,
    minHeight: 42,
    cursor: "not-allowed",
    userSelect: "none"
  },
  runButton: {
    border: "1px solid #cfd7ca",
    color: "#7a8977",
    backgroundColor: "#f2f5ee"
  },
  submitButton: {
    border: "1px solid #56735a",
    backgroundColor: "#56735a",
    color: "#f4f8f0",
    opacity: 0.65
  },
  executionNote: {
    color: "#7e897d",
    fontSize: 12,
    maxWidth: 360,
    lineHeight: 1.7
  },
  saveStatus: {
    display: "flex",
    alignItems: "center",
    gap: 7,
    color: "#69805f",
    fontSize: 11
  },
  saveError: {
    color: "#946a40"
  },
  results: {
    borderTop: "1px solid #dce1d9",
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column"
  },
  resultsHeading: {
    minHeight: 45,
    padding: "0 22px"
  },
  notRun: {
    fontSize: 10,
    padding: "2px 7px",
    borderRadius: 4,
    backgroundColor: "#f3f4f0",
    color: "#7d8678"
  },
  resultsBody: {
    padding: "16px 22px",
    overflowY: "auto",
    scrollbarWidth: "thin"
  },
  caseTabs: {
    display: "flex",
    gap: 6,
    marginBottom: 15
  },
  caseButton: {
    padding: "5px 11px",
    border: "1px solid transparent",
    borderRadius: 5,
    backgroundColor: "transparent",
    color: "#7d897c",
    fontSize: 11,
    cursor: "pointer",
    fontFamily: "inherit",
    userSelect: "none"
  },
  selectedCase: {
    backgroundColor: "#edf3e8",
    color: "#4a6944",
    borderColor: "#dce6d4",
    fontWeight: 600
  },
  caseData: {
    display: "grid",
    gridTemplateColumns: {
      default: "minmax(0, 1fr) minmax(0, 1fr)",
      "@media (max-width: 420px)": "minmax(0, 1fr)"
    },
    gap: 12
  },
  dataBlock: {
    backgroundColor: "#f7f8f4",
    border: "1px solid #ecefe7",
    padding: "9px 12px",
    borderRadius: 5,
    minWidth: 0
  },
  dataLabel: {
    fontSize: 10,
    fontWeight: 600,
    color: "#87917f",
    marginBottom: 4
  },
  dataValue: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 11,
    color: "#586b50",
    lineHeight: 1.9,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  resultsHint: {
    marginTop: 11,
    fontSize: 11,
    color: "#8c9386",
    display: "flex",
    alignItems: "center",
    gap: 7
  },
  screenReader: {
    position: "absolute",
    width: 1,
    height: 1,
    padding: 0,
    margin: -1,
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
    borderWidth: 0
  }
});
