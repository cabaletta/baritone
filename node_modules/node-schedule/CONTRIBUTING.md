## Rules
1. **No `--force` pushes** or modifying the git history in any way
2. Follow existing code style
3. Pull requests with tests are much more likely to be accepted
4. Follow the guidelines below

## Bugfix or Feature?

This project uses the [gitflow workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow). Simply put, you need to decide if your contribution will be a bug fix that could be released as a patch, or a feature that will end up being a minor or major release.

### Found a bug that can be fixed without affecting the API?

1. **Fork** this repo
2. Create a new branch from `master` to work in
3. **Add tests** if needed
4. Make sure your code **lints** by running `npm run lint`
5. Make sure your code **passes tests** by running `npm test`
6. Submit a **pull request** against the `master` branch

### New feature or anything that would result in a change to the API?

1. **Fork** this repo
2. Create a new branch from `develop` to work in
3. **Add tests** to as needed
4. Make sure your code **lints** by running `npm run lint`
5. Make sure your code **passes tests** by running `npm test`
6. Submit a **pull request** against the `develop` branch

## Releases

Declaring formal releases remains the prerogative of the project maintainer.
