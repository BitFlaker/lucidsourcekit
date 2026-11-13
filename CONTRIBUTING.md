# Contributing

Thank you for your interest in contributing to this project!
The best way to get started is by either opening an issue and asking what would be a good task to work on or by opening a PR for a bugfix or a small improvement.
Additionally, testing the app and opening issues for bugs you have found is always very helpful as well.

## Dependencies
The app is focused on only using the most essential dependencies.
This keeps the size of the app small and makes the app more self-contained.
Please do not add new dependencies to the project. Instead, open an issue clarifying why it would be required.

## PRs
PRs should be kept relatively small (e.g. fixes, small individual features) to make reviewing them easier and getting them merged faster.
Before implementing large features, first open an issue where the feature can be discussed.
Please test your code before making a PR and make sure the PR is up-to-date with the `master` branch.

## Programming language
This project is primarily written in Kotlin.
A few parts of the codebase are still written in Java, but new code should exclusively be written in Kotlin.

## AI usage
Usage of generative AI like LLMs is generally prohibited.
The following (non-exhaustive) list outlines the most notable rules:

* No AI-generated code or documentation is allowed
* AI must not be used for commenting code
* PRs co-authored or entirely created by AI will be rejected
* Usage of AI whose output only you see is permitted
  * Creating prototypes using AI to get across an idea is fine (as long as it does not end up in the codebase). This means, e.g. vibe coding an app and making a screen recording for showing how you would envision a feature to work, a redesign to look like, or a usage flow to behave.

Some reasons for this are to try and achieve the following:
* Learning and understanding (parts of) the existing codebase
* Knowing exactly what your code is doing as well as having thought about it thoroughly
* Keep the codebase at a manageable and maintainable size
