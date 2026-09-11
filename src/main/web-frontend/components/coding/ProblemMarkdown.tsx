import type {ComponentProps, ReactNode} from "react";
import * as stylex from "@stylexjs/stylex";
import Markdown, {type Components} from "react-markdown";

const styles = stylex.create({
  body: {
    color: "#bcbec4",
    lineHeight: 1.8,
    overflowWrap: "anywhere"
  },
  paragraph: {
    marginBottom: 14
  },
  heading: {
    fontSize: 14,
    fontWeight: 650,
    color: "#dfe1e5",
    marginTop: 24,
    marginBottom: 10
  },
  code: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: "0.9em",
    backgroundColor: "#393b40",
    borderRadius: 3
  },
  codeBlock: {
    padding: 12,
    marginBottom: 14,
    backgroundColor: "#1e1f22",
    borderRadius: 5,
    whiteSpace: "pre-wrap"
  },
  strong: {
    fontWeight: 650
  },
  emphasis: {
    fontStyle: "italic"
  },
  list: {
    paddingLeft: 24,
    marginBottom: 14,
    listStyleType: "disc"
  },
  orderedList: {
    listStyleType: "decimal",
    counterReset: "list-item"
  },
  listItem: {
    display: "list-item"
  },
  quote: {
    borderLeftWidth: 2,
    borderLeftStyle: "solid",
    borderLeftColor: "#4e5157",
    paddingLeft: 16,
    marginBottom: 14
  },
  separator: {
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#43454a",
    marginTop: 20,
    marginBottom: 20
  },
  lineBreak: {
    display: "block"
  },
  link: {
    color: "#b5ceff",
    textDecoration: "underline",
    cursor: "pointer"
  }
});

function MarkdownHeading({level, children}: {level: number; children?: ReactNode}) {
  return (
    <div sx={styles.heading} role="heading" aria-level={level}>
      {children}
    </div>
  );
}

function MarkdownLink({href, children}: ComponentProps<"a">) {
  if (!href) return <span>{children}</span>;

  function openLink() {
    window.open(href, "_blank", "noopener,noreferrer");
  }

  return (
    <span sx={styles.link} role="link" tabIndex={0} onClick={openLink} onKeyDown={(event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        openLink();
      }
    }}>
      {children}
    </span>
  );
}

const components: Components = {
  p: ({children}) => (
    <div sx={styles.paragraph}>
      {children}
    </div>
  ),
  h1: ({children}) => (
    <MarkdownHeading level={2}>
      {children}
    </MarkdownHeading>
  ),
  h2: ({children}) => (
    <MarkdownHeading level={2}>
      {children}
    </MarkdownHeading>
  ),
  h3: ({children}) => (
    <MarkdownHeading level={3}>
      {children}
    </MarkdownHeading>
  ),
  h4: ({children}) => (
    <MarkdownHeading level={4}>
      {children}
    </MarkdownHeading>
  ),
  h5: ({children}) => (
    <MarkdownHeading level={5}>
      {children}
    </MarkdownHeading>
  ),
  h6: ({children}) => (
    <MarkdownHeading level={6}>
      {children}
    </MarkdownHeading>
  ),
  code: ({children}) => (
    <span sx={styles.code}>
      {children}
    </span>
  ),
  pre: ({children}) => (
    <div sx={styles.codeBlock}>
      {children}
    </div>
  ),
  strong: ({children}) => (
    <span sx={styles.strong}>
      {children}
    </span>
  ),
  em: ({children}) => (
    <span sx={styles.emphasis}>
      {children}
    </span>
  ),
  ul: ({children}) => (
    <div sx={styles.list} role="list">
      {children}
    </div>
  ),
  ol: ({children}) => (
    <div sx={[styles.list, styles.orderedList]} role="list">
      {children}
    </div>
  ),
  li: ({children}) => (
    <div sx={styles.listItem} role="listitem">
      {children}
    </div>
  ),
  blockquote: ({children}) => (
    <div sx={styles.quote}>
      {children}
    </div>
  ),
  hr: () => (
    <div sx={styles.separator} role="separator" />
  ),
  br: () => (
    <span sx={styles.lineBreak} />
  ),
  a: MarkdownLink,
  img: ({alt}) => (
    <span>
      {alt}
    </span>
  )
};

export default function ProblemMarkdown({children}: {children: string}) {
  return <div sx={styles.body}><Markdown skipHtml components={components}>{children}</Markdown></div>;
}
