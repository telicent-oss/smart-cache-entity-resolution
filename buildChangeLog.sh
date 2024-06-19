#!/bin/bash -x
#
#   Copyright (c) Telicent Ltd.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

# Creates a CHANGELOG.md from Git or adds new release details to existing CHANGELOG.md
#
# ./buildChangeLog.sh (<release-tag>)
#
# If given no parameter it will go through each tag and get the commits within.
# It will place a link to the commit itself but also take a guess (through
# some sed parsing) at what Github issue to link too.
# If given a parameter, say "0.7.0", it will find the commits for that release
# tag and insert them into the top of the CHANGELOG.md.
#
# Note: this script should be revisited once a standardised approach to commits is
# agreed. Then we can be better about the links.
# Note: some validation / error-checking wouldn't go amiss either

changelog_file="CHANGELOG.md"
repository_url="https://github.com/Telicent-io/smart-cache-entity-resolution/"
temp_file="tmp.$$"

print_commit_logs() {
  local current_tag=$1
  local previous_tag=$2

  tag_date=$(git log -1 --pretty=format:'%ad' --date=short "$previous_tag")
  printf '## %s (%s)\n\n' "$previous_tag" "$tag_date"

  git log "$current_tag...$previous_tag" --no-merges --pretty=format:'*  %s [Details]('"${repository_url}"'commits/%H)' --reverse | grep -v Merge | grep -v "maven-release-plugin"
  if [ $? -ne 0 ]; then
    printf '* No commits for tag %s \n' "$previous_tag"
  fi

  printf "\n\n"
}

generate_changelog() {
  local previous_tag="0"

  for current_tag in $(git tag --sort=-creatordate); do
    if [ "$previous_tag" != "0" ]; then
      print_commit_logs "$current_tag" "$previous_tag"
    fi
    previous_tag="$current_tag"
  done

  tag_date=$(git log --reverse --pretty=format:'%ad' --date=short | head -1)
  printf '## %s (%s) \n\n' "${current_tag}" "${tag_date}"

  git log "${current_tag}" --no-merges --pretty=format:'*  %s [Details]('"${repository_url}"'commits/%H)' --reverse | grep -v Merge | grep -v "maven-release-plugin"
  if [ $? -ne 0 ]; then
    printf '* No commits for tag %s \n' "$current_tag"
  fi

  printf "\n\n"
}

update_changelog() {
  local parameter_tag="$1"

  for current_tag in $(git tag --sort=-creatordate); do
    if [ "$previous_tag" == "$parameter_tag" ]; then
      print_commit_logs "$current_tag" "$previous_tag"
    fi
    previous_tag="$current_tag"
  done
}

replace_issue_links() {
  local temp_file="$1"

  sed -i '' 's/\[.*[-:]\([0-9][0-9]*\)\]/[Issue \1](https:\/\/github\.com\/Telicent\-io\/smart\-cache\-entity\-resolution\/issue\/\1) /g' "$temp_file"
  sed -i '' 's/\[\([0-9][0-9]*\)\]/[Issue \1](https:\/\/github\.com\/Telicent\-io\/smart\-cache\-entity\-resolution\/issue\/\1) /g' "$temp_file"
}

update_file_with_entries() {
  local temp_file="$1"
  local temp_file_prefix="tmp_prefix.$$"

  touch "$changelog_file"
  cat "$temp_file" > "$temp_file_prefix"
  cat "$changelog_file" > "$temp_file"
  rm "$changelog_file"
  cat "$temp_file_prefix" "$temp_file" > "$changelog_file"
  rm "$temp_file_prefix" "$temp_file"
}

create_file_with_entries() {
  local temp_file="$1"

  touch "$changelog_file"
  rm "$changelog_file"
  cat "$temp_file" >> "$changelog_file"
  rm "$temp_file"
}

if [ -n "$1" ]; then
  update_changelog "$1" > "$temp_file"
  replace_issue_links "$temp_file"
  update_file_with_entries "$temp_file"
  echo "Changelog updated and saved to $changelog_file."
else
  generate_changelog > "$temp_file"
  replace_issue_links "$temp_file"
  create_file_with_entries "$temp_file"
  echo "Changelog generated and saved to $changelog_file."
fi


