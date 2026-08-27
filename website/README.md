# Website

This website is built using [Docusaurus](https://docusaurus.io/), a modern static website generator.

## Requirements

Use Node.js 20 or newer. CI currently uses Node.js 24.

## Installation

```shell
npm ci
```

## Local development

```shell
npm run start
```

This starts a local development server. Most changes are reflected without restarting it.

## Verification

```shell
npm run typecheck
npm run build
```

The production build generates static content in the `build` directory.

## Gradle build and deployment

The static version of the site is built using Gradle and deployed using GitHub Actions.

```shell
./gradlew aggregate-documentation:buildWebsite
```

Run this command from the repository root. It builds the Dokka API reference, type-checks Docusaurus, and writes the
deployable site to the
`aggregate-documentation/build/outputs/website/` directory.
