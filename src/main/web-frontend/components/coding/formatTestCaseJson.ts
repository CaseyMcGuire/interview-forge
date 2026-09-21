import beautify from "js-beautify";

/** Formats the original JSON text to preserve exact numbers. Invalid JSON is shown as is. */
export function formatTestCaseJson(value: string): string {
  try {
    JSON.parse(value);
  } catch {
    return value;
  }

  return beautify.js(value, {
    indent_size: 2,
    preserve_newlines: false
  });
}
