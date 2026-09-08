git tag -a "$(cat app/src/main/resources/version.txt)" -m "Release"
git push origin "$(cat app/src/main/resources/version.txt)"