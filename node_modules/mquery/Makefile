
test:
	@NODE_ENV=test ./node_modules/.bin/mocha $(T) $(TESTS)

test-cov:
	@NODE_ENV=test node \
		node_modules/.bin/istanbul cover \
		./node_modules/.bin/_mocha \
		-- -u exports \

open-cov:
	open coverage/lcov-report/index.html

lint:
	@NODE_ENV=test node ./node_modules/eslint/bin/eslint.js .

test-travis:
	@NODE_ENV=test node \
		node_modules/.bin/istanbul cover \
		./node_modules/.bin/_mocha \
		--report lcovonly \
		--bail
	@NODE_ENV=test node \
		./node_modules/eslint/bin/eslint.js .

.PHONY: test test-cov open-cov lint test-travis
