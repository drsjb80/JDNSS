# Coding Preferences

This file is for recording coding preferences, style choices, and project conventions.


## Python
- Use `python3` as the default Python interpreter.
- Prefer `black`-style formatting for Python code, but maintain existing project style where necessary.
- I don't use type hinting.

## General Style
- Use clear, concise logging messages.
- Keep functions small and focused.
- Do not have mutators/setters. Instead have custom constructors
- Prefer cloning to mutators/setters.
- I prefer to keep pre-existing comments along with AIs
- I want AI to run my unit and system tests, and create more when appropriate.
- I prefer yoda comparisons.

## JavaScript
- Always use strict equality operators: === and !==.

## CSS
- In CSS, prefer rem, em, en, and percentages for sizing and spacing.
- In CSS, use named colors instead of hex color values.

## Testing
- Unit test coverage target: 90%

## Git
- Always create a git hook that prompts for permission for commits over 10K.

## Error Handling

- Prefer explicit error handling over broad exception swallowing.
- Use `try/except/finally` when working with resources that must be cleaned up.
- Log exceptions at the appropriate level (`debug`, `warning`, `error`).

## Resource Management

- Use context managers (`with`) for file, socket, and temporary resource handling whenever practical.
- Ensure sockets and database sessions are closed cleanly.

## Database Access

- Use a lock around database writes to avoid concurrency issues.
- Prefer simple, explicit session management.

## Notes

- Record any repository-specific conventions here.
- Add preferences for new tools, formatting, or architectural decisions.

## Threading
- Don't busy wait.
- Don't have timeouts.
