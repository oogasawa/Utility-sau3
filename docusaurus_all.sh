#!/bin/bash

# Full reindex with mapping for both Japanese and English indices
java -jar target/Utility-sau3-4.0.0.jar sau:indexWithMapping -c w206_ja.conf,w206_en.conf -m docusaurus_ja_mapping.json,docusaurus_en_mapping.json
