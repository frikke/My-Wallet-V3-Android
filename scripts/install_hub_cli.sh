#!/usr/bin/env bash

unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=linux-amd64;;
    Darwin*)    machine=darwin-amd64;;
    CYGWIN*)    machine=windows-amd64;;
    *)          machine="UNKNOWN:${unameOut}"
esac

hubVersion=2.14.2
hubPackage=hub-${machine}-${hubVersion}

wget -q https://github.com/github/hub/releases/download/v${hubVersion}/${hubPackage}.tgz
tar zxf ${hubPackage}.tgz
sudo ./${hubPackage}/install
rm -rf ${hubPackage} ${hubPackage}.tgz

if [ -z $GH_USER_NAME ];
then
  echo "No github username provided as \$GH_USER_NAME"
  exit 1
fi

if [ -z $GH_USER_TOKEN ];
then
  echo "No github auth token provided as \$GH_USER_TOKEN"
  exit 1
fi

echo -e "github.com:" > ~/.config/hub
echo -e "- user: $GH_USER_NAME" >> ~/.config/hub
echo -e "  oauth_token: $GH_USER_TOKEN" >> ~/.config/hub
echo -e "  protocol: https" >> ~/.config/hub

echo -e "\ncreated ~/.config/hub:\n"
echo -e "\nHub is ready to use:"
hub --version
