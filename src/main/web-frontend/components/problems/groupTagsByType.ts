export function groupTagsByType<T extends {type: string}>(tags: ReadonlyArray<T>) {
  const topics: T[] = [];
  const companies: T[] = [];
  const other: T[] = [];

  for (const tag of tags) {
    switch (tag.type) {
      case "TOPIC":
        topics.push(tag);
        break;

      case "COMPANY":
        companies.push(tag);
        break;

      default:
        other.push(tag);
    }
  }

  return [
    {label: "Topics", tags: topics},
    {label: "Companies", tags: companies},
    {label: "Other tags", tags: other}
  ].filter((group) => group.tags.length > 0);
}
