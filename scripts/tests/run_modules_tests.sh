#!/usr/bin/env bash
printf 'Starting Modules Tests process...\n'

echo 'Navigating up to project root...'
cd ..

echo 'Building test command...'
tests=$(./gradlew generateModulesTestCommand |  sed -n 's/.*TEST_COMMAND//p')

printf "Running tests for:\n"
echo $tests

./gradlew $tests

if [ $? -eq 0 ]
then
  echo "Success: All tests passed."
  exit 0
else
  echo "Failure: One or more tests failed." >&2
  exit 1
fi