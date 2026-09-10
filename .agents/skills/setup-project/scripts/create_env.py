#!/usr/bin/env python3
"""Create local database configuration without overwriting files or printing secrets."""

import argparse
import os
from pathlib import Path
import re
import secrets
import sys


def identifier(value: str) -> str:
    if not re.fullmatch(r"[a-z_][a-z0-9_]{0,62}", value):
        raise argparse.ArgumentTypeError(
            "use a lowercase PostgreSQL identifier of at most 63 characters"
        )
    return value


def port_number(value: str) -> int:
    try:
        port = int(value)
    except ValueError:
        raise argparse.ArgumentTypeError("port must be an integer") from None
    if not 1 <= port <= 65535:
        raise argparse.ArgumentTypeError("port must be between 1 and 65535")
    return port


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--db-user", type=identifier, required=True)
    parser.add_argument("--db-name", type=identifier, required=True)
    parser.add_argument("--port", type=port_number, default=5432)
    args = parser.parse_args()

    root = args.repo_root.resolve()
    if not (root / "build.gradle.kts").is_file() or not (root / "package.json").is_file():
        parser.error("repo root must contain build.gradle.kts and package.json")

    target = root / ".env"
    contents = (
        f"DB_USER={args.db_user}\n"
        f"DB_PASSWORD={secrets.token_hex(24)}\n"
        f"DB_NAME={args.db_name}\n"
        f"DB_URL_PREFIX=jdbc:postgresql://localhost:{args.port}/\n"
    )
    try:
        descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        print("Refusing to overwrite an existing .env; validate and reuse it.", file=sys.stderr)
        return 1
    except OSError as error:
        print(f"Could not create .env: {error.strerror}", file=sys.stderr)
        return 1

    with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
        stream.write(contents)
    print("Created .env with private permissions. Credentials were not printed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
