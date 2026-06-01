# Contributing Guide

Thank you for taking the time to contribute! This project is free and open source,
licensed under the [GNU Affero General Public License v3.0 (AGPLv3)](LICENSE).

> ⚠️ **Before your first pull request is merged, you must sign our
> [Contributor License Agreement](CLA.md).** See the [CLA section](#contributor-license-agreement-cla)
> below for details.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Contributor License Agreement (CLA)](#contributor-license-agreement-cla)
3. [License & Compatibility](#license--compatibility)
4. [Getting Started](#getting-started)
5. [How to Contribute](#how-to-contribute)
   - [Reporting Bugs](#reporting-bugs)
   - [Suggesting Features](#suggesting-features)
   - [Submitting Code](#submitting-code)
6. [Development Workflow](#development-workflow)
7. [Coding Standards](#coding-standards)
8. [Commit Message Guidelines](#commit-message-guidelines)
9. [Pull Request Process](#pull-request-process)
10. [Community](#community)

---

## Code of Conduct

This project follows a Contributor Code of Conduct. By participating, you are expected
to uphold a welcoming and respectful environment for everyone. Please report unacceptable
behavior to the project maintainers at [maintainers@openfinova.com](mailto:maintainers@openfinova.com).

---

## Contributor License Agreement (CLA)

To keep this project legally clean and to allow the maintainer to offer commercial
licensing alongside the AGPLv3 open-source release, **all contributors must sign the
[Contributor License Agreement](CLA.md) before their first contribution is merged.**

### What the CLA means for you

- You **retain full copyright** over your contributions.
- You grant the project maintainer a **broad, royalty-free license** — including the
  right to use your work in commercial products or services.
- Your contributions remain available to everyone under the **AGPLv3** as part of this
  project.
- You are **not giving up** your own rights to use your contribution however you like.

### How to sign

We use **[CLA Assistant](https://cla-assistant.io/)** to handle signatures automatically.
When you open your first pull request, a bot will comment with a link to sign the CLA
electronically. The PR cannot be merged until the CLA is signed.

If you are contributing on behalf of a company or organisation, please also have an
authorised representative sign the **Corporate CLA** section in [CLA.md](CLA.md) and
email a copy to [maintainers@openfinova.com](mailto:maintainers@openfinova.com).

---

## License & Compatibility

This project is licensed under the **AGPLv3**. Key implications:

- Any modification — even when run as a network service — must be made available under
  the AGPLv3.
- If your contribution includes third-party code, its license must be **compatible with
  AGPLv3** before submitting.

> **Compatible licences:** MIT, Apache 2.0, GPL v2+, LGPL ✅  
> **Incompatible:** Proprietary or closed-source code ❌

---

## Getting Started

### Prerequisites

- [Git](https://git-scm.com/)
- The runtime/language required by this project (see `README.md`)
- A [GitHub](https://github.com) account

### Fork & Clone

```bash
# Fork the repo on GitHub, then clone YOUR fork:
git clone https://github.com/YOUR_USERNAME/minab-banking.git
cd minab-banking

# Add the upstream (official) repo as a remote
git remote add upstream https://github.com/openfinova/minab-banking.git
```

### Install Dependencies

```bash
# See README.md for project-specific setup instructions
```

---

## How to Contribute

### Reporting Bugs

Before opening an issue:
- Search [existing issues](https://github.com/openfinova/minab-banking/issues) to avoid duplicates.
- Make sure you are running the latest version.

When filing a bug report, please include:
- A clear, descriptive title.
- Steps to reproduce the problem.
- Expected vs. actual behavior.
- Your environment (OS, runtime version, etc.).
- Any relevant logs or screenshots.

### Suggesting Features

We welcome feature requests! Please open an issue with the label `enhancement` and describe:
- The problem your feature would solve.
- Your proposed solution or idea.
- Any alternatives you have considered.

### Submitting Code

1. Check the issue tracker for existing discussions before starting significant work.
2. For large changes, open an issue first to discuss the approach.
3. Fork the repository and create a feature branch (see [Development Workflow](#development-workflow)).
4. Write or update tests to cover your changes.
5. Sign the CLA when prompted by the bot on your first PR.
6. Open a pull request and fill in the PR template.

---

## Development Workflow

```bash
# Keep your fork up to date
git fetch upstream
git checkout main
git merge upstream/main

# Create a branch for your work
git checkout -b feat/my-new-feature

# ... make your changes ...

# Run tests locally before pushing
./mvnw --batch-mode verify

# Push and open a PR
git push origin feat/my-new-feature
```

Branch naming conventions:

| Prefix        | Purpose                                 |
|---------------|-----------------------------------------|
| `feat/`       | New feature                             |
| `fix/`        | Bug fix                                 |
| `docs/`       | Documentation only                      |
| `refactor/`   | Code restructuring, no behaviour change |
| `test/`       | Adding or updating tests                |
| `chore/`      | Build scripts, CI, dependencies         |

---

## Coding Standards

- Follow the existing style and conventions in the codebase.
- Keep functions and modules focused and small.
- Add comments where the intent is not immediately obvious.
- Update or add documentation for any public APIs you change.
- Ensure no new linting warnings are introduced.

Run the linter before submitting:  
```bash
./mvnw --batch-mode formatter:validate
```

---

## Commit Message Guidelines

We follow a simplified [Conventional Commits](https://www.conventionalcommits.org/) style:

```
<type>(<scope>): <short summary>

[optional body]

[optional footer: e.g., Closes #123]
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

**Examples:**

```
feat(auth): add OAuth2 login support

fix(api): handle null response from upstream service

Closes #42
```

Rules:
- Use the **imperative mood** in the summary ("add" not "added").
- Keep the summary line under **72 characters**.
- Reference related issues in the footer when applicable.

---

## Pull Request Process

1. **Draft PRs are welcome** for early feedback — just mark them as draft.
2. Ensure the **CLA bot check is green** — PRs cannot be merged without a signed CLA.
3. Ensure all other CI checks pass before requesting review.
4. At least **one maintainer approval** is required to merge.
5. A maintainer may request changes — please respond or update within a reasonable timeframe.
6. Squash commits if the history is noisy before merging (maintainers may do this on merge).
7. Delete your branch after the PR is merged.

---

## Community

- 💬 **Discussions:** [GitHub Discussions](https://github.com/openfinova/minab-banking/discussions)
- 🐛 **Issues:** [GitHub Issues](https://github.com/openfinova/minab-banking/issues)
- 📧 **Contact:** [maintainers@openfinova.com](mailto:maintainers@openfinova.com)

We appreciate every contribution — from fixing a typo to implementing a major feature.
Thank you for helping make this project better!