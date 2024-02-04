#!/bin/bash


# Japanese

curl -X DELETE "http://localhost:9200/docusaurus_ja"
curl -X PUT -H "Content-Type: application/json" -d @docusaurus_ja_mapping.json http://localhost:9200/docusaurus_ja | jq .
java -jar target/Utility-sau3-1.1.0-fat.jar docusaurus:index -c docusaurus_ja.conf

# English

curl -X DELETE "http://localhost:9200/docusaurus_en"
curl -X PUT -H "Content-Type: application/json" -d @docusaurus_en_mapping.json http://localhost:9200/docusaurus_en | jq .
java -jar target/Utility-sau3-1.1.0-fat.jar docusaurus:index -c docusaurus_en.conf

