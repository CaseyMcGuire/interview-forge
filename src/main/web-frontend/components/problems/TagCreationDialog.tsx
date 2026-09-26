import {useEffect, useId, useRef, useState, type FormEvent} from "react";
import {graphql, useMutation} from "react-relay";
import type {RecordSourceSelectorProxy} from "relay-runtime";
import * as stylex from "@stylexjs/stylex";
import type {TagCreationDialogMutation, TagType} from "__generated__/TagCreationDialogMutation.graphql";
import ProblemCreationField from "./ProblemCreationField";

type Props = {
  onCreated: (tagId: string) => void;
  onClose: () => void;
};

type FieldError = {
  field: string;
  message: string;
};

const tagTypeOptions = [
  {type: "TOPIC", label: "Topic"},
  {type: "COMPANY", label: "Company"}
] as const;

const styles = stylex.create({
  dialog: {
    width: "min(480px, calc(100vw - 32px))",
    maxWidth: "none",
    maxHeight: "calc(100dvh - 32px)",
    margin: "auto",
    padding: 24,
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 10,
    backgroundColor: {
      default: "#1e1f22",
      "::backdrop": "rgb(0 0 0 / 60%)"
    },
    color: "#dfe1e5",
    boxShadow: "0 20px 60px rgb(0 0 0 / 40%)"
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 16
  },
  title: {
    fontSize: 18,
    fontWeight: 600
  },
  description: {
    color: "#9da0a8",
    fontSize: 13
  },
  typeField: {
    borderWidth: 0,
    margin: 0,
    padding: 0,
    minWidth: 0
  },
  typeLegend: {
    padding: 0,
    marginBottom: 8,
    fontWeight: 600
  },
  typeOptions: {
    display: "flex",
    gap: 24
  },
  typeOption: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    fontSize: 14
  },
  radio: {
    width: 16,
    height: 16,
    accentColor: "#6b9bfa",
    outlineColor: "#6b9bfa",
    outlineOffset: 3
  },
  error: {
    color: "#f2a6a6",
    fontSize: 13
  },
  actions: {
    display: "flex",
    justifyContent: "flex-end",
    gap: 8
  },
  button: {
    padding: "8px 14px",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: "inherit",
    fontSize: 13,
    cursor: "pointer",
    outlineColor: "#6b9bfa",
    outlineOffset: 2
  },
  primary: {
    borderColor: "#3574f0",
    backgroundColor: "#3574f0",
    color: "#ffffff"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

function addCreatedTagToCatalog(store: RecordSourceSelectorProxy) {
  const result = store.getRootField("createTag");
  if (result?.getType() !== "CreateTagSuccess") {
    return;
  }

  const tag = result.getLinkedRecord("tag");
  if (!tag) {
    return;
  }

  // The catalog is a plain list, so Relay's connection insertion directives cannot update it.
  const root = store.getRoot();
  const tags = (root.getLinkedRecords("tags") ?? []).filter((item) => item != null);
  const updatedTags = [...tags.filter((item) => item.getDataID() !== tag.getDataID()), tag];
  updatedTags.sort((left, right) => (
    String(left.getValue("displayName")).localeCompare(String(right.getValue("displayName"))) ||
    String(left.getValue("slug")).localeCompare(String(right.getValue("slug")))
  ));
  root.setLinkedRecords(updatedTags, "tags");
}

export default function TagCreationDialog(props: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const submitting = useRef(false);
  const id = useId();
  const [displayName, setDisplayName] = useState("");
  const [slug, setSlug] = useState("");
  const [type, setType] = useState<TagType>("TOPIC");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<ReadonlyArray<FieldError>>([]);

  const [commit, isInFlight] = useMutation<TagCreationDialogMutation>(graphql`
    mutation TagCreationDialogMutation($input: CreateTagInput!) {
      createTag(input: $input) {
        __typename
        ... on CreateTagSuccess {
          tag {
            id
            slug
            displayName
            type
          }
        }
        ... on TagValidationFailure {
          message
          fieldErrors {
            field
            message
          }
        }
        ... on TagForbidden {
          message
        }
      }
    }
  `);

  useEffect(() => {
    const dialog = dialogRef.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);

  function close() {
    if (submitting.current) {
      return;
    }

    props.onClose();
  }

  function createTag(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) {
      return;
    }

    submitting.current = true;
    setError(null);
    setFieldErrors([]);

    commit({
      variables: {input: {slug, displayName, type}},

      updater: addCreatedTagToCatalog,

      onCompleted: (response, graphqlErrors) => {
        submitting.current = false;
        const result = response.createTag;

        if (graphqlErrors?.length || !result) {
          setError("The tag could not be created. Please try again.");
          return;
        }

        switch (result.__typename) {
          case "CreateTagSuccess":
            props.onCreated(result.tag.id);
            break;

          case "TagValidationFailure":
            setFieldErrors(result.fieldErrors);
            setError(result.message);
            break;

          case "TagForbidden":
            setError(result.message);
            break;

          default:
            setError("The tag could not be created. Please try again.");
        }
      },

      onError: () => {
        submitting.current = false;
        setError("The request failed. Your inputs are still here; please try again.");
      }
    });
  }

  return (
    <dialog
      ref={dialogRef}
      sx={styles.dialog}
      aria-labelledby={`${id}-title`}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
    >
      <form sx={styles.form} onSubmit={createTag} aria-busy={isInFlight}>
        <h2 id={`${id}-title`} sx={styles.title}>Create tag</h2>
        <p sx={styles.description}>New tags are available to all problems. Save the problem to assign your selections.</p>

        <fieldset sx={[styles.typeField, isInFlight && styles.disabled]} disabled={isInFlight}>
          <legend sx={styles.typeLegend}>Type</legend>
          <div sx={styles.typeOptions}>
            {tagTypeOptions.map((option) => (
              <label key={option.type} sx={styles.typeOption}>
                <input
                  sx={styles.radio}
                  type="radio"
                  name={`${id}-type`}
                  value={option.type}
                  checked={type === option.type}
                  onChange={() => setType(option.type)}
                />
                {option.label}
              </label>
            ))}
          </div>
        </fieldset>

        <ProblemCreationField
          label="Display name"
          value={displayName}
          onChange={setDisplayName}
          error={fieldErrors.find((item) => item.field === "displayName")?.message}
          maxLength={100}
          disabled={isInFlight}
        />
        <ProblemCreationField
          label="Slug"
          value={slug}
          onChange={setSlug}
          hint="Use lowercase letters, numbers, and hyphens, such as two-pointer. The slug cannot be changed later."
          error={fieldErrors.find((item) => item.field === "slug")?.message}
          maxLength={100}
          disabled={isInFlight}
        />

        {error && <p sx={styles.error} role="alert">{error}</p>}

        <div sx={styles.actions}>
          <button
            type="button"
            sx={[styles.button, isInFlight && styles.disabled]}
            disabled={isInFlight}
            onClick={close}
          >
            Cancel
          </button>
          <button
            type="submit"
            sx={[styles.button, styles.primary, isInFlight && styles.disabled]}
            disabled={isInFlight}
          >
            {isInFlight ? "Creating…" : "Create tag"}
          </button>
        </div>
      </form>
    </dialog>
  );
}
